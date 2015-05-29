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

import io.wallee.protocol.{ MalformedMqttPacketException, PingReq }

import scala.util.{ Failure, Success, Try }

/** [[MqttPacketDecoder]] for [[PingReq]] packets.
 */
object PingReqDecoder extends MqttPacketDecoder[PingReq](PacketType.Pingreq) {

  override protected[this] def doDecode(frame: MqttFrame): Try[PingReq] = {
    if (frame.fixedHeaderFlags != 0x00) {
      Failure(MalformedMqttPacketException("[MQTT-2.2.2-2]: Invalid CONNECT header flags"))
    } else if (frame.variableHeaderPlusPayload.nonEmpty) {
      Failure(MalformedMqttPacketException(s"Invalid payload size ${frame.variableHeaderPlusPayload.size} - expected 0"))
    } else {
      Success(PingReq())
    }
  }
}
