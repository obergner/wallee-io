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

import akka.util.CompactByteString
import io.wallee.protocol.{ MalformedMqttPacketException, QoS }
import org.scalatest.{ FlatSpec, Matchers }

import scala.util.{ Failure, Success }

class MqttPacketEncoderSpec extends FlatSpec with Matchers {

  "MqttPacketEncoder#encodeUint16 when given a uint < 0" should "report an error" in {
    MqttPacketEncoder.encodeUint16(-1) match {
      case Success(_)  => fail("Should have reported an error for input < 0")
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
    }
  }

  "MqttPacketEncoder#encodeUint16 when given a uint > max allowed value" should "report an error" in {
    MqttPacketEncoder.encodeUint16(scala.math.pow(2, 16).toInt + 1) match {
      case Success(_)  => fail("Should have reported an error for input > max allowed value")
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
    }
  }

  "MqttPacketEncoder#encodeUint16 when given a uint between 0 and 255" should "correctly encode that uint" in {
    val expectedResult = CompactByteString(0x00, 0xFF)
    MqttPacketEncoder.encodeUint16(255) match {
      case Success(actualResult) => assert(actualResult == expectedResult)
      case Failure(_)            => fail("Should not have reported an error for legal input")
    }
  }

  "MqttPacketEncoder#encodeUint16 when given a uint between 256 and 65536" should "correctly encode that uint" in {
    val expectedResult = CompactByteString(0xFF, 0xFF)
    MqttPacketEncoder.encodeUint16(65535) match {
      case Success(actualResult) => assert(actualResult == expectedResult)
      case Failure(_)            => fail("Should not have reported an error for legal input")
    }
  }

  "MqttPacketEncoder#encodeUtf8 when given an empty string" should "correctly encode it" in {
    val expectedResult = CompactByteString(0x00, 0x00)
    MqttPacketEncoder.encodeUtf8("") match {
      case Success(actualResult) => assert(actualResult == expectedResult)
      case Failure(_)            => fail("Should not have reported an error for legal input")
    }
  }

  "MqttPacketEncoder#encodeUtf8 when given a short unicode string" should "correctly encode it" in {
    val expectedResult = CompactByteString(0x00, 0x05, 0x41, 0xF0, 0xAA, 0x9B, 0x94)
    MqttPacketEncoder.encodeUtf8("A\uD869\uDED4") match {
      case Success(actualResult) => assert(actualResult == expectedResult)
      case Failure(_)            => fail("Should not have reported an error for legal input")
    }
  }

  "MqttPacketEncoder#encodeRemainingLength when given a remaining length < 0" should "report an error" in {
    MqttPacketEncoder.encodeRemainingLength(-1) match {
      case Success(_)  => fail("Should have reported an error for input < 0")
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
    }
  }

  "MqttPacketEncoder#encodeRemainingLength when given a remaining length > max allowed value" should "report an error" in {
    MqttPacketEncoder.encodeRemainingLength(MqttPacketEncoder.MaxRemainingLength + 1) match {
      case Success(_)  => fail("Should have reported an error for input > max allowed value")
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
    }
  }

  "MqttPacketEncoder#encodeRemainingLength when given a remaining length between 0 and 127" should "correctly encode it" in {
    val expectedResult = CompactByteString(0x7F)
    MqttPacketEncoder.encodeRemainingLength(127) match {
      case Success(actualResult) => assert(actualResult == expectedResult)
      case Failure(ex)           => fail("Should not have reported an error for legal input")
    }
  }

  "MqttPacketEncoder#encodeRemainingLength when given a remaining length needing two bytes encoding" should "correctly encode it" in {
    val remainingLength = 65 + 34 * 128
    val expectedResult = CompactByteString(0xC1, 0x22)
    MqttPacketEncoder.encodeRemainingLength(remainingLength) match {
      case Success(actualResult) => assert(actualResult == expectedResult)
      case Failure(ex)           => fail("Should not have reported an error for legal input")
    }
  }

  "MqttPacketEncoder#encodeRemainingLength when given a remaining length needing three bytes encoding" should "correctly encode it" in {
    val remainingLength = 12 + 88 * 128 + 77 * (128 * 128)
    val expectedResult = CompactByteString(0x8C, 0xD8, 0x4D)
    MqttPacketEncoder.encodeRemainingLength(remainingLength) match {
      case Success(actualResult) => assert(actualResult == expectedResult)
      case Failure(ex)           => fail("Should not have reported an error for legal input")
    }
  }

  "MqttPacketEncoder#encodeRemainingLength when given a remaining length needing four bytes encoding" should "correctly encode it" in {
    val remainingLength = 126 + 55 * 128 + 112 * (128 * 128) + 33 * (128 * 128 * 128)
    val expectedResult = CompactByteString(0xFE, 0xB7, 0xF0, 0x21)
    MqttPacketEncoder.encodeRemainingLength(remainingLength) match {
      case Success(actualResult) => assert(actualResult == expectedResult)
      case Failure(ex)           => fail("Should not have reported an error for legal input")
    }
  }

  "MqttPacketEncoder#encodeQoSInto when given QoS AtMostOnce" should "correctly encode it" in {
    val qos = QoS.AtMostOnce
    val target = 0xFD // 11111101
    val leftShift = 2

    val expectedResult = 0xF1 // 11110001

    val actualResult = MqttPacketEncoder.encodeQoSInto(qos, target, leftShift)

    assert(actualResult == expectedResult)
  }

  "MqttPacketEncoder#encodeQoSInto when given QoS AtLeastOnce" should "correctly encode it" in {
    val qos = QoS.AtLeastOnce
    val target = 0xFF // 11111111
    val leftShift = 3

    val expectedResult = 0xEF // 11101111

    val actualResult = MqttPacketEncoder.encodeQoSInto(qos, target, leftShift)

    assert(actualResult == expectedResult)
  }

  "MqttPacketEncoder#encodeQoSInto when given QoS ExactlyOnce" should "correctly encode it" in {
    val qos = QoS.ExactlyOnce
    val target = 0xA5 // 10100101
    val leftShift = 5

    val expectedResult = 0xC5 // 11000101

    val actualResult = MqttPacketEncoder.encodeQoSInto(qos, target, leftShift)

    assert(actualResult == expectedResult)
  }

  "MqttPacketEncoder#encodeQoSInto when given QoS Reserved" should "correctly encode it" in {
    val qos = QoS.Reserved
    val target = 0xA5 // 10100101
    val leftShift = 4

    val expectedResult = 0xB5 // 10110101

    val actualResult = MqttPacketEncoder.encodeQoSInto(qos, target, leftShift)

    assert(actualResult == expectedResult)
  }
}
