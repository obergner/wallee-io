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
import io.wallee.protocol.{ Connack, ConnectionAccepted, MalformedMqttPacketException }
import org.scalatest.{ FlatSpec, Matchers }

import scala.util.{ Failure, Success, Try }

class MqttPacketDecoderSpec extends FlatSpec with Matchers {

  class TestMqttPacketDecoder extends MqttPacketDecoder[Connack] {

    override def decode(frame: MqttFrame): Connack = Connack(sessionPresent = false, ConnectionAccepted)

    override def decodeUint16(payload: ByteString): Try[(Int, ByteString)] = super.decodeUint16(payload)

    override def decodeUtf8String(payload: ByteString): Try[(String, ByteString)] = super.decodeUtf8String(payload)
  }

  val underTest = new TestMqttPacketDecoder

  "An MqttPacketDecoder when decoding a Uint16 from a size 0 ByteString" should "report an error" in {
    underTest.decodeUint16(ByteString.empty) match {
      case Success(_)  => fail("should have failed given an empty ByteString")
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
    }
  }

  "An MqttPacketDecoder when decoding a Uint16 from a size 1 ByteString" should "report an error" in {
    underTest.decodeUint16(CompactByteString(0x01)) match {
      case Success(_)  => fail("should have failed given a size 1 ByteString")
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
    }
  }

  "An MqttPacketDecoder when decoding a Uint16 from a size 2 ByteString" should "return the correctly decoded unsigned integer" in {
    val expectedResult: Int = 129 + 175 * 256
    underTest.decodeUint16(CompactByteString(0x81, 0xAF)) match {
      case Success((actualResult, _)) => assert(actualResult == expectedResult)
      case Failure(_)                 => fail("should not have reported an error since input is legal")
    }
  }

  "An MqttPacketDecoder when decoding a Uint16 from a size 5 ByteString" should "return the rest of the input ByteString" in {
    val inputBuffer = CompactByteString(0x01, 0x02, 0x03, 0x04, 0x05)
    val expectedResult = inputBuffer.drop(2)
    underTest.decodeUint16(inputBuffer) match {
      case Success((_, actualResult)) => assert(actualResult == expectedResult)
      case Failure(_)                 => fail("should not have reported an error since input is legal")
    }
  }

  "An MqttPacketDecoder when decoding a utf-8 string from a size 0 ByteString" should "report an error" in {
    underTest.decodeUtf8String(ByteString.empty) match {
      case Success(_)  => fail("should have failed given an empty ByteString")
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
    }
  }

  "An MqttPacketDecoder when decoding a utf-8 string from a size 1 ByteString" should "report an error" in {
    underTest.decodeUtf8String(CompactByteString(0x01)) match {
      case Success(_)  => fail("should have failed given a size 1 ByteString")
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
    }
  }

  "An MqttPacketDecoder when decoding a utf-8 string from a ByteString of insufficient size" should "report an error" in {
    underTest.decodeUtf8String(CompactByteString(0x05, 0x00, 0x61, 0x61, 0x61, 0x61)) match {
      case Success(_)  => fail("should have failed given a size 1 ByteString")
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
    }
  }

  "An MqttPacketDecoder when decoding a utf-8 string from a size 2 ByteString" should "return an empty string" in {
    underTest.decodeUtf8String(CompactByteString(0x00, 0x00)) match {
      case Success((string, _)) => assert(string == "")
      case Failure(ex)          => fail("should have returned a decoded UTF-8 string")
    }
  }

  "An MqttPacketDecoder when decoding a size 5 utf-8 string from a size 8 ByteString" should "return the rest of the input ByteString" in {
    val inputBuffer = CompactByteString(0x05, 0x00, 0x61, 0x61, 0x61, 0x61, 0x61, 0x00, 0x00, 0x00)
    val expectedResult = inputBuffer.drop(7)
    underTest.decodeUtf8String(inputBuffer) match {
      case Success((_, actualResult)) => assert(actualResult == expectedResult)
      case Failure(ex)                => fail("should have returned a decoded UTF-8 string")
    }
  }
}
