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
import io.wallee.protocol.PingResp

import scala.util.{ Success, Try }

/** [[MqttPacketEncoder]] for [[PingResp]] packets.
 */
object PingRespEncoder extends MqttPacketEncoder[PingResp] {

  private[this] val Type = PacketType.Pingresp << 4

  private[this] val ZeroLength = 0x00

  override def encode(packet: PingResp): Try[ByteString] = {
    Success(CompactByteString(Type, ZeroLength))
  }
}
