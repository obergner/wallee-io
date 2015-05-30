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
import akka.stream.stage.{ Context, PushStage, SyncDirective }
import io.wallee.protocol.MqttPacket
import io.wallee.shared.logging.TcpConnectionLogging

import scala.util.{ Failure, Success }

/** Transform [[MqttFrame]]s into [[MqttPacket]]s.
 */
final class DecoderStage(protected[this] val connection: Tcp.IncomingConnection)(protected[this] implicit val system: ActorSystem)
    extends PushStage[MqttFrame, MqttPacket] with TcpConnectionLogging {

  override def onPush(elem: MqttFrame, ctx: Context[MqttPacket]): SyncDirective = {
    log.debug(s"DECODE:  $elem ...")
    val decodedPacket = MqttPacketDecoder.decode(elem)
    log.debug(s"DECODED: $elem -> $decodedPacket")
    decodedPacket match {
      case Success(packet) => ctx.push(packet)
      case Failure(ex)     => ctx.fail(ex)
    }
  }
}
