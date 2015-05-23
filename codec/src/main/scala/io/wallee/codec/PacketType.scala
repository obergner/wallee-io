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

/** List all MQTT 3.1.1 packet types.
 *
 *  @see http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718021
 */
object PacketType {

  val Reserved1: Byte = 0x00

  val Connect: Byte = 0x01

  val Connack: Byte = 0x02

  val Publish: Byte = 0x03

  val Puback: Byte = 0x04

  val Pubrec: Byte = 0x05

  val Pubrel: Byte = 0x06

  val Pubcomp: Byte = 0x07

  val Subscribe: Byte = 0x08

  val Suback: Byte = 0x09

  val Unsubscribe: Byte = 0x0A

  val Unsuback: Byte = 0x0B

  val Pingreq: Byte = 0x0C

  val Pingresp: Byte = 0x0D

  val Disconnect: Byte = 0x0E

  val Reserved2: Byte = 0x0F

  def parse(firstFixedHeaderByte: Byte): Byte = (firstFixedHeaderByte >> 4).toByte
}
