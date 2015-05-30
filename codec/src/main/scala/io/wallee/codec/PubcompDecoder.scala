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

import io.wallee.protocol.{ MalformedMqttPacketException, PacketIdentifier, Pubcomp }

import scala.util.{ Failure, Try }

/** [[MqttPacketDecoder]] for [[Pubcomp]] packets.
 */
object PubcompDecoder extends MqttPacketDecoder[Pubcomp](PacketType.Pubcomp) {

  import MqttPacketDecoder._

  override protected[this] def doDecode(frame: MqttFrame): Try[Pubcomp] = {
    if (frame.fixedHeaderFlags != 0x00) {
      Failure(MalformedMqttPacketException("[MQTT-2.2.2-2]: Invalid PUBACK header flags"))
    } else {
      decodeUint16(frame.variableHeaderPlusPayload).map[Pubcomp] { case (packetId, remainder) => Pubcomp(PacketIdentifier(packetId)) }
    }
  }
}
