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
    if (frame.fixedHeaderFlags != 0x00) {
      Failure(MalformedMqttPacketException("[MQTT-2.2.2-2]: Invalid CONNECT header flags"))
    } else {
      VariableConnectHeader.decode(frame.variableHeaderPlusPayload).flatMap[Connect] {
        case (variableHeader, remainder) =>
          new StatefulConnectPacketDecoder(variableHeader).decode(remainder)
      }
    }
  }
}

@SuppressWarnings(Array("org.wartremover.warts.Var"))
protected[codec] final class StatefulConnectPacketDecoder(val variableHeader: VariableConnectHeader) {

  import MqttPacketDecoder._

  private[this] var clientId: String = ""

  private[this] var willTopic: Option[Topic] = None

  private[this] var willMessage: Option[String] = None

  private[this] var username: Option[String] = None

  private[this] var password: Option[String] = None

  def decode(payload: ByteString): Try[Connect] = {
    decodeUtf8String(payload)
      .flatMap[(Option[Topic], ByteString)](decodeWillTopic)
      .flatMap[(Option[String], ByteString)](decodeWillMessage)
      .flatMap[(Option[String], ByteString)](decodeUsername)
      .flatMap[(Option[String], ByteString)](decodePassword)
      .map[Connect]({
        case (passwd, remainder) =>
          this.password = passwd
          Connect(
            variableHeader.protocolName,
            variableHeader.protocolLevel,
            clientId,
            variableHeader.cleanSession,
            variableHeader.keepAliveSecs,
            username,
            password,
            variableHeader.willQoS,
            variableHeader.willRetain,
            willTopic,
            willMessage)
      })
  }

  private[this] def decodeWillTopic: ((String, ByteString)) => Try[(Option[Topic], ByteString)] = {
    case (cltId, remainder) =>
      this.clientId = cltId
      if (variableHeader.willFlag) {
        decodeUtf8String(remainder).map[(Option[Topic], ByteString)] { case (topicString, rem) => (Some(Topic(topicString)), rem) }
      } else {
        Success((None, remainder))
      }
  }

  private[this] def decodeWillMessage: ((Option[Topic], ByteString)) => Try[(Option[String], ByteString)] = {
    case (willTpc, remainder) =>
      this.willTopic = willTpc
      if (variableHeader.willFlag) {
        decodeUtf8String(remainder).map[(Option[String], ByteString)] { case (messageString, rem) => (Some(messageString), rem) }
      } else {
        Success((None, remainder))
      }
  }

  private[this] def decodeUsername: ((Option[String], ByteString)) => Try[(Option[String], ByteString)] = {
    case (willMsg, remainder) =>
      this.willMessage = willMsg
      if (variableHeader.username) {
        decodeUtf8String(remainder).map[(Option[String], ByteString)] { case (usernameString, rem) => (Some(usernameString), rem) }
      } else {
        Success((None, remainder))
      }
  }

  private[this] def decodePassword: ((Option[String], ByteString)) => Try[(Option[String], ByteString)] = {
    case (usernm, remainder) =>
      this.username = usernm
      if (variableHeader.password) {
        decodeUtf8String(remainder).map[(Option[String], ByteString)] { case (passwordString, rem) => (Some(passwordString), rem) }
      } else {
        Success((None, remainder))
      }
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
    keepAliveSecs: Int)

protected[codec] object VariableConnectHeader {

  import MqttPacketDecoder._

  private val Level4Value: Int = 0x04

  private val UsernameFlag: Int = 0x80

  private val PasswordFlag: Int = 0x40

  private val WillRetainFlag: Int = 0x20

  private val WillQoSMask: Int = 0x18

  private val WillQoSLeftShift: Int = 3

  private val WillFlagFlag: Int = 0x04

  private val CleanSessionFlag: Int = 0x02

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

  private def decodeConstantLengthPayload(protocolName: String, restBuffer: ByteString): Try[(VariableConnectHeader, ByteString)] = {
    val protocolLevel = if (restBuffer(0) == Level4Value) ProtocolLevel.Level4 else ProtocolLevel.UnsupportedProtocolLevel
    val username = (restBuffer(1) & UsernameFlag) == UsernameFlag
    val password = (restBuffer(1) & PasswordFlag) == PasswordFlag
    val willRetain = (restBuffer(1) & WillRetainFlag) == WillRetainFlag
    val willQoSByte = (restBuffer(1) & WillQoSMask) >> WillQoSLeftShift
    val willQoS = decodeQoS(willQoSByte)
    val willFlag = (restBuffer(1) & WillFlagFlag) == WillFlagFlag
    val cleanSession = (restBuffer(1) & CleanSessionFlag) == CleanSessionFlag
    decodeUint16(restBuffer.drop(2)) match {
      case Success((keepAliveSecs, remainder)) => Success(
        (VariableConnectHeader(protocolName, protocolLevel, username, password, willRetain, willQoS, willFlag, cleanSession, keepAliveSecs), remainder))
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
    case AtMostOncePat  => QoS.AtMostOnce
    case AtLeastOncePat => QoS.AtLeastOnce
    case ExactlyOncePat => QoS.ExactlyOnce
    case _              => QoS.Reserved
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
