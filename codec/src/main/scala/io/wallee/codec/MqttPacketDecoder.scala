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
import io.wallee.protocol.{ MalformedMqttPacketException, MqttPacket, QoS }

import scala.util.{ Failure, Success, Try }

/** Decode an [[MqttFrame]] into an [[MqttPacket]].
 *
 *  @tparam P Concrete [[MqttPacket]] type
 */
abstract class MqttPacketDecoder[+P <: MqttPacket](packetType: Byte) {

  /** Decode `frame`, returning the decoded [[MqttPacket]].
   *
   *  @param frame [[MqttFrame]] to decode
   *  @return Decoded [[MqttPacket]]
   *  @throws IllegalArgumentException If this [[MqttPacketDecoder]] is not capable of handling `frame`, i.e. `frame`
   *                                  contains a serialized packet of incompatible type
   */
  @throws[IllegalArgumentException]
  final def decode(frame: MqttFrame): Try[P] = {
    require(frame.packetType == packetType, s"Unsupported packet type: ${frame.packetType} - supported: $packetType")
    doDecode(frame)
  }

  protected[this] def doDecode(frame: MqttFrame): Try[P]
}

object MqttPacketDecoder {

  val QoSMask: Int = 0x03

  val AtMostOnceVal: Int = 0x00

  val AtLeastOnceVal: Int = 0x01

  val ExactlyOnceVal: Int = 0x02

  /** Decode `frame` into an [[MqttPacket]].
   *
   *  @param frame [[MqttFrame]] to decode
   *  @return Decoded [[MqttPacket]], or failure
   */
  def decode(frame: MqttFrame): Try[MqttPacket] = frame.packetType match {
    case PacketType.Connect   => ConnectDecoder.decode(frame)
    case PacketType.Publish   => PublishDecoder.decode(frame)
    case PacketType.Puback    => PubackDecoder.decode(frame)
    case PacketType.Pubrec    => PubrecDecoder.decode(frame)
    case PacketType.Pubrel    => PubrelDecoder.decode(frame)
    case PacketType.Pubcomp   => PubcompDecoder.decode(frame)
    case PacketType.Subscribe => SubscribeDecoder.decode(frame)
    case PacketType.Pingreq   => PingReqDecoder.decode(frame)
    case _                    => Failure(new IllegalArgumentException(s"Unsupported packet type: ${frame.packetType}"))
  }

  protected[codec] def decodeUint16(payload: ByteString): Try[(Int, ByteString)] = {
    if (payload.size < 2) {
      Failure(MalformedMqttPacketException("Insufficient space left for encoding Uint16"))
    } else {
      val msb = 0xFF & payload(0).toInt
      val lsb = 0xFF & payload(1).toInt
      val uint16: Int = lsb + (msb << 8)
      Success((uint16, payload.drop(2)))
    }
  }

  protected[codec] def decodeUtf8String(payload: ByteString): Try[(String, ByteString)] = {
    decodeUint16(payload).flatMap[(String, ByteString)] {
      case (length, restPayload) =>
        if (restPayload.size < length) {
          Failure(MalformedMqttPacketException(s"Insufficient space left for encoding string of size $length"))
        } else {
          val (string, rest) = restPayload.splitAt(length)
          Success((string.utf8String, rest))
        }
    }
  }

  protected[codec] def decodeQoS(encoded: Int): QoS = encoded & QoSMask match {
    case AtMostOnceVal  => QoS.AtMostOnce
    case AtLeastOnceVal => QoS.AtLeastOnce
    case ExactlyOnceVal => QoS.ExactlyOnce
    case _              => QoS.Reserved
  }
}
