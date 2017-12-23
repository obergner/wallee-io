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

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.Tcp
import akka.stream.stage._
import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import io.wallee.protocol.MqttPacket
import io.wallee.shared.logging.TcpConnectionLogging

/** [[GraphStage]] for logging incoming/outgoing [[MqttPacket]]s.
 *
 *  @param logPrefix Prefix to prepend to each log message, typically one of "RCVD" and "SEND"
 *  @param level     Log level to use when logging network packets
 */
final class LogMqttPackets(
    protected[this] val connection: Tcp.IncomingConnection,
    logPrefix:                      String, level: Logging.LogLevel)(protected[this] implicit val system: ActorSystem)
  extends GraphStage[FlowShape[MqttPacket, MqttPacket]] with TcpConnectionLogging {

  val in: Inlet[MqttPacket] = Inlet[MqttPacket]("LogMqttPackets.in")

  val out: Outlet[MqttPacket] = Outlet[MqttPacket]("LogMqttPackets.out")

  override def shape: FlowShape[MqttPacket, MqttPacket] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      setHandler(in, new InHandler {
        @throws[Exception](classOf[Exception])
        override def onPush(): Unit = {
          val elem = grab(in)
          log.log(level, s"$logPrefix: $elem")
          push(out, elem)
        }
      })

      setHandler(out, new OutHandler {
        @throws[Exception](classOf[Exception])
        override def onPull(): Unit = {
          pull(in)
        }
      })
    }
}
