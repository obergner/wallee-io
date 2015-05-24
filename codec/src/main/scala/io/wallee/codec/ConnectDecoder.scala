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
import io.wallee.protocol._

import scala.util.{ Failure, Success, Try }

/** [[MqttPacketDecoder]] for [[Connect]] packets.
 */
object ConnectDecoder extends MqttPacketDecoder[Connect](PacketType.Connect) {

  override protected[this] def doDecode(frame: MqttFrame): Try[Connect] = {
    Failure(new NotImplementedError())
  }
}

protected[codec] final case class VariableConnectHeader(
  protocolName:  String,
  protocolLevel: ProtocolLevel,
  username:      Boolean,
  password:      Boolean,
  willRetain:    Boolean,
  willQoS:       QoS,
  willFlag:      Boolean,
  cleanSession:  Boolean,
  keepAliveSecs: Int
)

protected[codec] object VariableConnectHeader {

  import MqttPacketDecoder._

  private[this] val Level4Value: Int = 0x04

  private[this] val UsernameFlag: Int = 0x80

  private[this] val PasswordFlag: Int = 0x40

  private[this] val WillRetainFlag: Int = 0x20

  private[this] val WillQoSMask: Int = 0x18

  private[this] val WillQoSLeftShift: Int = 3

  private[this] val WillFlagFlag: Int = 0x04

  private[this] val CleanSessionFlag: Int = 0x02

  private[this] val AtMostOncePat: Int = 0x00

  private[this] val AtLeastOncePat: Int = 0x01

  private[this] val ExactlyOncePat: Int = 0x02

  def decode(buffer: ByteString): Try[(VariableConnectHeader, ByteString)] = {
    decodeUtf8String(buffer).flatMap[(VariableConnectHeader, ByteString)] {
      case (protocolName, restBuffer) =>
        if (restBuffer.size < 4) {
          Failure(MalformedMqttPacketException("CONNECT variable header truncated"))
        } else {
          decodeConstantLengthPayload(protocolName, restBuffer)
        }
    }
  }

  private[this] def decodeConstantLengthPayload(protocolName: String, restBuffer: ByteString): Try[(VariableConnectHeader, ByteString)] = {
    val protocolLevel = if (restBuffer(0) == Level4Value) Level4 else UnsupportedProtocolLevel
    val username = (restBuffer(1) & UsernameFlag) == UsernameFlag
    val password = (restBuffer(1) & PasswordFlag) == PasswordFlag
    val willRetain = (restBuffer(1) & WillRetainFlag) == WillRetainFlag
    val willQoSByte = (restBuffer(1) & WillQoSMask) >> WillQoSLeftShift
    val willQoS = willQoSByte match {
      case AtMostOncePat  => AtMostOnce
      case AtLeastOncePat => AtLeastOnce
      case ExactlyOncePat => ExactlyOnce
      case _              => Reserved
    }
    val willFlag = (restBuffer(1) & WillFlagFlag) == WillFlagFlag
    val cleanSession = (restBuffer(1) & CleanSessionFlag) == CleanSessionFlag
    decodeUint16(restBuffer.drop(2)) match {
      case Success((keepAliveSecs, remainder)) => Success(
        (VariableConnectHeader(protocolName, protocolLevel, username, password, willRetain, willQoS, willFlag, cleanSession, keepAliveSecs), remainder)
      )
      case Failure(ex) => Failure(ex)
    }
  }
}

/** Fixed packet header. Present in all MQTT packets, yet relevant only for PUBLISH packages.
 *
 *  @param packetTypeAndFlags First byte in fixed header
 *
 *  @see http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718022
 *
 *  @todo Move to PublishDecoder
 */
protected[codec] final class FixedHeaderFlags(packetTypeAndFlags: Byte) {

  import FixedHeaderFlags._

  def dup: Boolean = (packetTypeAndFlags & DupFlag) == DupFlag

  def qos: QoS = packetTypeAndFlags & QoSMask match {
    case AtMostOncePat  => AtMostOnce
    case AtLeastOncePat => AtLeastOnce
    case ExactlyOncePat => ExactlyOnce
    case _              => Reserved
  }

  def retain: Boolean = (packetTypeAndFlags & RetainFlag) == RetainFlag
}

protected[codec] object FixedHeaderFlags {

  val DupFlag: Int = 0x08

  val QoSMask: Int = 0x06

  val AtMostOncePat: Int = 0x00

  val AtLeastOncePat: Int = 0x02

  val ExactlyOncePat: Int = 0x04

  val RetainFlag: Int = 0x01
}
