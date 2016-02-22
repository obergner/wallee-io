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

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.config.Config
import io.wallee.codec.{ DecoderStage, EncoderStage, FrameDecoderStage }
import io.wallee.connection.auth.ConnectProcessor
import io.wallee.connection.monitor.{ LogMqttPackets, LogNetworkPackets }
import io.wallee.connection.ping.PingReqProcessor
import io.wallee.connection.publish.PublishProcessor
import io.wallee.connection.{ MqttConnectionFactory, ProtocolHandler }
import io.wallee.spi.auth.AuthenticationPlugin

/**
 */
class DefaultMqttConnectionFactory(
  config:               Config,
  authenticationPlugin: AuthenticationPlugin
)(protected[this] implicit val system: ActorSystem)
    extends MqttConnectionFactory {

  override def apply(conn: Tcp.IncomingConnection): Flow[ByteString, ByteString, NotUsed] =
    Flow.fromGraph[ByteString, ByteString, NotUsed](GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      system.log.debug(s"[${conn.remoteAddress}] Creating connection handler pipeline ...")

      val input = builder.add(Flow[ByteString])
      val output = builder.add(Flow[ByteString])

      input.out
        .via(new LogNetworkPackets(conn, "RCVD", Logging.DebugLevel))
        .via(new FrameDecoderStage(conn))
        .via(new DecoderStage(conn))
        .via(new LogMqttPackets(conn, "RCVD", Logging.DebugLevel))
        .via(protocolHandler(conn))
        .via(new LogMqttPackets(conn, "SEND", Logging.DebugLevel))
        .via(new EncoderStage(conn))
        .via(new LogNetworkPackets(conn, "SEND", Logging.DebugLevel))
        .~>(output.in)

      system.log.debug(s"[${conn.remoteAddress}] Connection handler pipeline created")

      FlowShape(input.in, output.out)
    })

  private[this] def protocolHandler(conn: Tcp.IncomingConnection): ProtocolHandler = {
    new ProtocolHandler(new ConnectProcessor(conn, authenticationPlugin), new PingReqProcessor(conn), new PublishProcessor(conn), conn)
  }
}
