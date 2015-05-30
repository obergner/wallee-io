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
import io.wallee.protocol.Publish

import scala.util.{ Failure, Success, Try }

/** Encode [[Publish]] packages.
 */
object PublishEncoder extends MqttPacketEncoder[Publish] {

  import MqttPacketEncoder._

  private[this] val NoVal: Int = 0x00

  private[this] val DupVal: Int = 0x08

  private[this] val RetainVal: Int = 0x01

  override def encode(packet: Publish): Try[ByteString] = {
    val packetType = PacketType.Publish << 4
    val packetTypeAndDup = packetType | (if (packet.dup) DupVal else NoVal)
    val packetTypeDupAndQos = encodeQoSInto(packet.qosLevel, packetTypeAndDup, 1)
    val packetTypeAndFlags = packetTypeDupAndQos | (if (packet.retain) RetainVal else NoVal)

    encodeRemainingLength(packet.remainingLength)
      .map[ByteString]({ remainingLength => CompactByteString(packetTypeAndFlags) ++ remainingLength })
      .flatMap[ByteString]({ buffer =>
        encodeUtf8(packet.topic) match {
          case Success(buf) => Success(buffer ++ buf)
          case Failure(ex)  => Failure(ex)
        }
      })
      .flatMap[ByteString]({ buffer =>
        encodeUint16(packet.packetId.id) match {
          case Success(buf) => Success(buffer ++ buf)
          case Failure(ex)  => Failure(ex)
        }
      })
      .map[ByteString]({ buffer => buffer ++ packet.applicationMessage })
  }

}
