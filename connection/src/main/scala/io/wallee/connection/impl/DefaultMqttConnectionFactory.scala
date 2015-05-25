/*
 * Copyright 2015 Olaf Bergner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.wallee.connection.impl

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{ Flow, FlowGraph, Tcp }
import akka.util.ByteString
import com.typesafe.config.Config
import io.wallee.codec.{ DecoderStage, EncoderStage, FrameDecoderStage, MqttFrame }
import io.wallee.connection.MqttConnectionFactory
import io.wallee.connection.monitor.{ LogMqttPackets, LogNetworkPackets }
import io.wallee.protocol.{ Connack, ConnectReturnCode, MqttPacket }

/**
 */
class DefaultMqttConnectionFactory(config: Config, protected[this] implicit val system: ActorSystem)
    extends MqttConnectionFactory {

  override def apply(conn: Tcp.IncomingConnection): Flow[ByteString, ByteString, _] = Flow() { implicit builder: FlowGraph.Builder[Unit] =>
    import FlowGraph.Implicits._

    val input = builder.add(Flow[ByteString])
    val output = builder.add(Flow[ByteString])

    input.outlet
      .transform[ByteString](() => new LogNetworkPackets(conn, "RCVD", Logging.DebugLevel))
      .transform[MqttFrame](() => new FrameDecoderStage)
      .transform[MqttPacket](() => new DecoderStage)
      .transform[MqttPacket](() => new LogMqttPackets(conn, "RCVD", Logging.DebugLevel))
      .map[MqttPacket](_ => Connack(sessionPresent = true, ConnectReturnCode.ConnectionAccepted))
      .transform[MqttPacket](() => new LogMqttPackets(conn, "SEND", Logging.DebugLevel))
      .transform[ByteString](() => new EncoderStage)
      .transform[ByteString](() => new LogNetworkPackets(conn, "SEND", Logging.DebugLevel))
      .~>(output.inlet)

    (input.inlet, output.outlet)
  }
}
