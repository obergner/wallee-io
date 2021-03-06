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

import akka.util.{ ByteString, CompactByteString }
import io.wallee.protocol.Puback

import scala.util.{ Failure, Success, Try }

/** Encode [[Puback]] packages.
 */
object PubackEncoder extends MqttPacketEncoder[Puback] {

  import MqttPacketEncoder._

  override def encode(packet: Puback): Try[ByteString] = {
    val packetTypeAndFlags = CompactByteString(PacketType.Puback << 4)
    encodeRemainingLength(packet.remainingLength).flatMap[ByteString] { remainingLength =>
      encodeUint16(packet.packetId.id) match {
        case Success(packetId) => Success(packetTypeAndFlags ++ remainingLength ++ packetId)
        case Failure(ex)       => Failure(ex)
      }
    }
  }
}
