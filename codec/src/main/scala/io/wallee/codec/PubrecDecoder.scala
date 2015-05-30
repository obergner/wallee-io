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

import io.wallee.protocol.{ MalformedMqttPacketException, PacketIdentifier, Pubrec }

import scala.util.{ Failure, Try }

/** [[MqttPacketDecoder]] for [[Pubrec]] packets.
 */
object PubrecDecoder extends MqttPacketDecoder[Pubrec](PacketType.Pubrec) {

  import MqttPacketDecoder._

  override protected[this] def doDecode(frame: MqttFrame): Try[Pubrec] = {
    if (frame.fixedHeaderFlags != 0x00) {
      Failure(MalformedMqttPacketException("[MQTT-2.2.2-2]: Invalid PUBREC header flags"))
    } else {
      decodeUint16(frame.variableHeaderPlusPayload).map[Pubrec] { case (packetId, remainder) => Pubrec(PacketIdentifier(packetId)) }
    }
  }
}
