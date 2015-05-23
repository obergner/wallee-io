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

import akka.util.ByteString

/**
 * A chunk of bytes that contains exactly one serialized MQTT packet.
 *
 * An `MqttFrame` knows via its `packetType` property about the type of MQTT packet it contains. Likewise, its
 * `fixedHeaderFlags` property offers access to bits 0-3 in the first fixed header byte.
 *
 * Finally, an `MqttFrame`'s `variableHeaderPlusPayload` property returns a `ByteString` containing variable header (if
 * present) and payload in serialized form.
 *
 * @see http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718020
 */
final class MqttFrame(private val typeAndFlags: Byte, val variableHeaderPlusPayload: ByteString) {
  require(
    variableHeaderPlusPayload.size <= RemainingLengthDecoder.MaxRemainingLength,
    s"Variable header plus payload exceeds maximum allowed remaining length " +
      s"of ${RemainingLengthDecoder.MaxRemainingLength} bytes: ${variableHeaderPlusPayload.size}"
  )

  /**
   * Return a byte representation of ths MQTT packet's type.
   *
   * @return A byte representation of ths MQTT packet's type
   */
  def packetType: Byte = PacketType.parse(typeAndFlags)

  /**
   * Return this MQTT packet's header flags. Relevant only for PUBLISH packets.
   *
   * @return This MQTT packet's header flags
   */
  def fixedHeaderFlags: Byte = (typeAndFlags & 0x0F).toByte

  override def equals(other: Any): Boolean = other match {
    case that: MqttFrame =>
      typeAndFlags == that.typeAndFlags && variableHeaderPlusPayload == that.variableHeaderPlusPayload
    case _ => false
  }

  override def hashCode(): Int = {
    31 * typeAndFlags.hashCode() + variableHeaderPlusPayload.hashCode()
  }
}
