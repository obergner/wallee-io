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
import io.wallee.protocol.{ PacketIdentifier, Publish, Topic }

import scala.util.Try

/** An [[MqttPacketDecoder]] for [[Publish]] packets.
 *
 *  @see [[http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718037]]
 */
object PublishDecoder extends MqttPacketDecoder[Publish](PacketType.Publish) {

  import MqttPacketDecoder._

  private[this] val DupMask: Int = 0x08

  private[this] val RetainMask: Int = 0x01

  override protected[this] def doDecode(frame: MqttFrame): Try[Publish] = {
    decodeUtf8String(frame.variableHeaderPlusPayload).flatMap[(Topic, PacketIdentifier, ByteString)]({
      case (topic, remainder) =>
        decodeUint16(remainder).map[(Topic, PacketIdentifier, ByteString)]({
          case (packetId, rem) =>
            (Topic(topic), PacketIdentifier(packetId), rem)
        })
    }).map[Publish]({
      case (topic, packetId, remainder) =>
        val dup = (frame.fixedHeaderFlags & DupMask) == DupMask
        val qos = decodeQoS(frame.fixedHeaderFlags >> 1)
        val retain = (frame.fixedHeaderFlags & RetainMask) == RetainMask
        Publish(dup, qos, retain, topic, packetId, remainder)
    })
  }
}
