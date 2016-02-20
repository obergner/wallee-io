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

package io.wallee.codec

import akka.actor.ActorSystem
import akka.stream.scaladsl.Tcp
import akka.stream.stage._
import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import io.wallee.protocol.MqttPacket
import io.wallee.shared.logging.TcpConnectionLogging

import scala.util.{ Failure, Success }

/** Transform [[MqttFrame]]s into [[MqttPacket]]s.
 */
final class DecoderStage(protected[this] val connection: Tcp.IncomingConnection)(protected[this] implicit val system: ActorSystem)
    extends GraphStage[FlowShape[MqttFrame, MqttPacket]] with TcpConnectionLogging {

  val in = Inlet[MqttFrame]("DecoderStage.in")

  val out = Outlet[MqttPacket]("DecoderStage.out")

  override def shape: FlowShape[MqttFrame, MqttPacket] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      setHandler(in, new InHandler {
        @throws[Exception](classOf[Exception])
        override def onPush(): Unit = {
          val elem = grab(in)
          log.debug(s"DECODE:  $elem ...")
          val decodedPacket = MqttPacketDecoder.decode(elem)
          log.debug(s"DECODED: $elem -> $decodedPacket")
          decodedPacket match {
            case Success(packet) => push(out, packet)
            case Failure(ex) =>
              log.error(ex, s"Failed to decode MQTT frame: ${ex.getMessage}")
              fail(out, ex)
          }
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
