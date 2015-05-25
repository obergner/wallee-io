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
import io.wallee.protocol.{ Connack, ConnectReturnCode }

import scala.util.Try

/** Encode [[Connack]] packages.
 */
object ConnackEncoder extends MqttPacketEncoder[Connack] {

  import MqttPacketEncoder._

  private val NoSessionPresent: Byte = 0x00

  private val SessionPresent: Byte = 0x01

  private val ConnectionAcceptedByte: Byte = 0x00

  private val UnacceptableProtocolVersionByte: Byte = 0x01

  private val IdentifierRejectedByte: Byte = 0x02

  private val ServerUnavailableByte: Byte = 0x03

  private val BadUsernameOrPasswordByte: Byte = 0x04

  private val NotAuthorizedByte: Byte = 0x05

  override def encode(packet: Connack): Try[ByteString] = {
    val packetTypeAndFlags = CompactByteString(PacketType.Connack << 4)
    encodeRemainingLength(packet.remainingLength).map[ByteString] { buffer =>
      packetTypeAndFlags ++ buffer ++ CompactByteString(if (packet.sessionPresent) SessionPresent else NoSessionPresent, encodeReturnCode(packet.returnCode))
    }
  }

  private def encodeReturnCode(returnCode: ConnectReturnCode): Byte = returnCode match {
    case ConnectReturnCode.ConnectionAccepted          => ConnectionAcceptedByte
    case ConnectReturnCode.UnacceptableProtocolVersion => UnacceptableProtocolVersionByte
    case ConnectReturnCode.IdentifierRejected          => IdentifierRejectedByte
    case ConnectReturnCode.ServerUnavailable           => ServerUnavailableByte
    case ConnectReturnCode.BadUsernameOrPassword       => BadUsernameOrPasswordByte
    case ConnectReturnCode.NotAuthorized               => NotAuthorizedByte
  }
}
