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

import akka.stream.stage.{ Context, PushStage, SyncDirective }
import akka.util.ByteString
import io.wallee.protocol.MqttPacket

import scala.util.{ Failure, Success }

/** Flow stage for encoding [[MqttPacket]]s.
 */
class EncoderStage extends PushStage[MqttPacket, ByteString] {

  override def onPush(elem: MqttPacket, ctx: Context[ByteString]): SyncDirective = {
    MqttPacketEncoder.encode(elem) match {
      case Success(buffer) => ctx.push(buffer)
      case Failure(ex)     => ctx.fail(ex)
    }
  }
}
