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

package io.wallee.connection.monitor

import java.nio.ByteOrder

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.BidiShape
import akka.stream.scaladsl.{ BidiFlow, Flow, GraphDSL, Tcp }
import io.wallee.protocol.MqttPacket

/** Created by obergner on 24.02.16.
 */
object MqttPacketsLogging {

  def apply(conn: Tcp.IncomingConnection, level: Logging.LogLevel)(implicit system: ActorSystem): BidiFlow[MqttPacket, MqttPacket, MqttPacket, MqttPacket, NotUsed] =
    BidiFlow.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      implicit val order: ByteOrder = ByteOrder.LITTLE_ENDIAN

      val outbound = builder.add(Flow[MqttPacket].via(new LogMqttPackets(conn, "SEND", level)(system)))
      val inbound = builder.add(Flow[MqttPacket].via(new LogMqttPackets(conn, "RCVD", level)(system)))

      BidiShape.fromFlows(outbound, inbound)
    })
}
