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

package io.wallee.protocol

/** Abstract base class for MQTT packets.
 */
abstract class MqttPacket {
  requireWellformed(
    remainingLength <= MqttPacket.MaxRemainingLength,
    s"MQTT 3.1.1: an MQTT packet's remaining length MUST NOT exceed ${MqttPacket.MaxRemainingLength}")

  /** This MQTT packet's length in bytes on the wire.
   *
   *  @return Length in bytes on the wire
   *  @todo Consider removing this method
   */
  def lengthInBytes: Int = {
    val remainingLengthBytes: Int = (scala.math.log(remainingLength) / scala.math.log(MqttPacket.RemainingLengthBase)).floor.toInt + 1
    1 + remainingLengthBytes + remainingLength
  }

  /** This MQTT packet's remaining length, i.e. its length on the wire sans its fixed header.
   *
   *  @return Length on the wire sans fixed header
   */
  def remainingLength: Int
}

object MqttPacket {

  /** In MQTT 3.1.1, remaining length is encoded using a base of 128.
   *
   *  @see http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718023
   */
  val RemainingLengthBase: Int = 128

  /** Maximum remaining length allowed by MQTT 3.1.1.
   *
   *  @see http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718023
   */
  val MaxRemainingLength: Int = 127 * 128 * 128 * 128 + 127 * 128 * 128 + 127 * 128 + 127
}
