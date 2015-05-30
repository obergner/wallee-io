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

import java.nio.charset.Charset

import akka.util.{ ByteString, CompactByteString }
import io.wallee.protocol._

import scala.util.{ Failure, Success, Try }

/** Base class for [[MqttPacket]] encoders.
 */
abstract class MqttPacketEncoder[P <: MqttPacket] {

  def encode(packet: P): Try[ByteString]
}

object MqttPacketEncoder {

  /** Encode MQTT `packet`.
   *
   *  @param packet [[MqttPacket]] to encode
   *  @tparam P Concrete [[MqttPacket]] type parameter
   *  @return Encoded `packet`, or failure
   */
  def encode[P <: MqttPacket](packet: P): Try[ByteString] = packet match {
    case p: Connack  => ConnackEncoder.encode(p)
    case p: Publish  => PublishEncoder.encode(p)
    case p: Puback   => PubackEncoder.encode(p)
    case p: Pubrec   => PubrecEncoder.encode(p)
    case p: Pubrel   => PubrelEncoder.encode(p)
    case p: PingResp => PingRespEncoder.encode(p)
    case _           => Failure(new IllegalArgumentException(s"Unsupported MQTT packet: $packet"))
  }

  protected[codec] val MaxUint16: Int = scala.math.pow(2, 16).floor.toInt

  protected[codec] val MaxRemainingLength: Int = 127 + 127 * 128 + 127 * (128 * 128) + 127 * (128 * 128 * 128)

  protected[codec] val Utf8: Charset = Charset.forName("UTF-8")

  protected[codec] def encodeUint16(uint16: Int): Try[ByteString] = {
    if (uint16 < 0 || uint16 > MaxUint16) {
      Failure(MalformedMqttPacketException(s"Uint16 not in allowed range 0 - $MaxUint16: $uint16"))
    } else {
      val msb = (uint16 & 0xFF00) >> 8
      val lsb = uint16 & 0x00FF
      Success(CompactByteString(msb, lsb))
    }
  }

  protected[codec] def encodeUtf8(string: String): Try[ByteString] = encodeUint16(string.getBytes(Utf8).length).map[ByteString] {
    buf => buf ++ CompactByteString(string.getBytes(Utf8))
  }

  protected[codec] def encodeRemainingLength(remainingLength: Int): Try[ByteString] = {
    if (remainingLength < 0 || remainingLength > MaxRemainingLength) {
      Failure(MalformedMqttPacketException(s"Remaining length not in allowed range 0 - $MaxRemainingLength: $remainingLength"))
    } else {
      Success(doEncodeRemainingLength(remainingLength, ByteString.empty))
    }
  }

  private def doEncodeRemainingLength(remainingLength: Int, buffer: ByteString): ByteString = {
    if (remainingLength <= 0) {
      assert(buffer.size <= 4)
      buffer
    } else {
      val thisDigit = remainingLength % 128
      val restRemainingLength = remainingLength / 128
      val encodedThisDigit = if (restRemainingLength > 0) thisDigit + 0x80 else thisDigit
      doEncodeRemainingLength(restRemainingLength, buffer ++ CompactByteString(encodedThisDigit))
    }
  }

  protected[codec] def encodeQoSInto(qos: QoS, target: Int, leftShift: Int): Int = {
    val stanceMask: Int = ~(0x03 << leftShift)
    val mask: Int = qos match {
      case QoS.AtMostOnce  => 0x00 << leftShift
      case QoS.AtLeastOnce => 0x01 << leftShift
      case QoS.ExactlyOnce => 0x02 << leftShift
      case QoS.Reserved    => 0x03 << leftShift
    }
    (target & stanceMask) | mask
  }
}
