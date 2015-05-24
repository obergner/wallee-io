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
import io.wallee.protocol.{ MalformedMqttPacketException, ProtocolLevel, QoS }
import org.scalatest.{ FlatSpec, Matchers }

import scala.util.{ Failure, Success }

class VariableConnectHeaderSpec extends FlatSpec with Matchers {

  "VariableConnectHeader#decode when given an empty input buffer" should "report an error" in {
    VariableConnectHeader.decode(ByteString.empty) match {
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
      case Success(_)  => fail("Should have returned an error given empty input")
    }
  }

  "VariableConnectHeader#decode when given an input buffer without protocol level byte" should "report an error" in {
    val inputBuffer = CompactByteString(0x00, 0x04, 0x4D, 0x51, 0x54, 0x54)
    VariableConnectHeader.decode(inputBuffer) match {
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
      case Success(_)  => fail("Should have returned an error given input without flags byte")
    }
  }

  "VariableConnectHeader#decode when given an input buffer without flags byte" should "report an error" in {
    val inputBuffer = CompactByteString(0x00, 0x04, 0x4D, 0x51, 0x54, 0x54, 0x04)
    VariableConnectHeader.decode(inputBuffer) match {
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
      case Success(_)  => fail("Should have returned an error given input without flags byte")
    }
  }

  "VariableConnectHeader#decode when given an input buffer without keep alive seconds bytes" should "report an error" in {
    val inputBuffer = CompactByteString(0x00, 0x04, 0x4D, 0x51, 0x54, 0x54, 0x04, 0x00)
    VariableConnectHeader.decode(inputBuffer) match {
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
      case Success(_)  => fail("Should have returned an error given input without keep alive seconds bytes")
    }
  }

  "VariableConnectHeader#decode when given an input buffer with only one keep alive seconds byte" should "report an error" in {
    val inputBuffer = CompactByteString(0x00, 0x04, 0x4D, 0x51, 0x54, 0x54, 0x04, 0x00, 0x00)
    VariableConnectHeader.decode(inputBuffer) match {
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
      case Success(_)  => fail("Should have returned an error given input with only one keep alive seconds byte")
    }
  }

  "VariableConnectHeader#decode when given an input buffer containing a well-formed variable header" should "correctly decode variable header" in {
    val expectedProtocolName = "MQTT"
    val expectedProtocolLevel = ProtocolLevel.Level4
    val expectedUsername = true
    val expectedPassword = true
    val expectedWillRetain = true
    val expectedWillQoS = QoS.AtLeastOnce
    val expectedWillFlag = true
    val expectedCleanSession = true
    val expectedKeepAliveSecs = 0x80
    val expectedResult = VariableConnectHeader(
      expectedProtocolName,
      expectedProtocolLevel,
      expectedUsername,
      expectedPassword,
      expectedWillRetain,
      expectedWillQoS,
      expectedWillFlag,
      expectedCleanSession,
      expectedKeepAliveSecs
    )
    val inputBuffer = CompactByteString(0x00, 0x04, 0x4D, 0x51, 0x54, 0x54, 0x04, 0xEF, 0x00, 0x80)
    VariableConnectHeader.decode(inputBuffer) match {
      case Failure(_)                 => fail("Should not have returned Failure")
      case Success((actualResult, _)) => assert(actualResult == expectedResult)
    }
  }

  "VariableConnectHeader#decode when given an input buffer containing a well-formed variable header" should "return unconsumed input buffer rest" in {
    val inputBuffer = CompactByteString(0x00, 0x04, 0x4D, 0x51, 0x54, 0x54, 0x04, 0xEF, 0x00, 0x80, 0x01, 0x02)
    VariableConnectHeader.decode(inputBuffer) match {
      case Failure(_)              => fail("Should not have returned Failure")
      case Success((_, remainder)) => assert(remainder == inputBuffer.drop(10))
    }
  }
}
