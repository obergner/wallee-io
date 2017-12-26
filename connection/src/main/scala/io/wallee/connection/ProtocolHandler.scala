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

package io.wallee.connection

import akka.actor.ActorSystem
import akka.stream.scaladsl.Tcp
import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import io.wallee.connection.auth.ConnectProcessor
import io.wallee.connection.ping.PingReqProcessor
import io.wallee.connection.publish.PublishProcessor
import io.wallee.protocol.{ Connect, MqttPacket, PingReq, Publish }
import io.wallee.shared.logging.TcpConnectionLogging

/** [[GraphStage]] for logging incoming/outgoing [[MqttPacket]]s.
 */
final class ProtocolHandler(
    connectProcessor:               ConnectProcessor,
    pingReqProcessor:               PingReqProcessor,
    publishProcessor:               PublishProcessor,
    protected[this] val connection: Tcp.IncomingConnection)(protected[this] implicit val system: ActorSystem)
  extends GraphStage[FlowShape[MqttPacket, MqttPacket]] with TcpConnectionLogging {

  val in: Inlet[MqttPacket] = Inlet[MqttPacket]("ProtocolHandler.in")

  val out: Outlet[MqttPacket] = Outlet[MqttPacket]("ProtocolHandler.out")

  override def shape: FlowShape[MqttPacket, MqttPacket] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      setHandler(in, new InHandler {
        @throws[Exception](classOf[Exception])
        override def onPush(): Unit = {
          val elem = grab(in)
          val optionalResponse: Option[MqttPacket] = elem match {
            case p: Connect => connectProcessor.process(p)
            case p: PingReq => pingReqProcessor.process(p)
            case p: Publish => publishProcessor.process(p)
            case _          => None
          }
          optionalResponse foreach { resp => push(out, resp) }
        }
      })

      setHandler(out, new OutHandler {
        @throws[Exception](classOf[Exception])
        override def onPull(): Unit = pull(in)
      })
    }
}
