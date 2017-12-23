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
import io.wallee.protocol._
import org.scalatest.{ FlatSpec, Matchers }

import scala.util.{ Failure, Success }

class ConnectDecoderSpec extends FlatSpec with Matchers {

  "ConnectDecoder given a serialized CONNECT packet containing illegal fixed header flags" should "report an error" in {
    // A telltale case of unabashed robbery: https://github.com/surgemq/surgemq/blob/master/message/connect_test.go
    val typeAndFlags: Int = 0x11
    val variableHeaderPlusPayload = CompactByteString(
      0, // Length MSB (0)
      4, // Length LSB (4)
      'M', 'Q', 'T', 'T',
      4, // Protocol level 4
      206, // connect flags 11001110, will QoS = 01, cleanSession = true, willRetain = false
      0, // Keep Alive MSB (0)
      10, // Keep Alive LSB (10)
      0, // Client ID MSB (0)
      7, // Client ID LSB (7)
      's', 'u', 'r', 'g', 'e', 'm', 'q',
      0, // Will Topic MSB (0)
      4, // Will Topic LSB (4)
      'w', 'i', 'l', 'l',
      0, // Will Message MSB (0)
      12, // Will Message LSB (12)
      's', 'e', 'n', 'd', ' ', 'm', 'e', ' ', 'h', 'o', 'm', 'e',
      0, // Username ID MSB (0)
      7, // Username ID LSB (7)
      's', 'u', 'r', 'g', 'e', 'm', 'q',
      0, // Password ID MSB (0)
      10, // Password ID LSB (10)
      'v', 'e', 'r', 'y', 's', 'e', 'c', 'r', 'e', 't')
    val frame = new MqttFrame(typeAndFlags.toByte, variableHeaderPlusPayload)

    ConnectDecoder.decode(frame) match {
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
      case Success(_)  => fail("Should have failed since CONNECT packet contains illegal fixed header flags")
    }
  }

  "ConnectDecoder given a serialized CONNECT packet containing a truncated password" should "report an error" in {
    // A telltale case of unabashed robbery: https://github.com/surgemq/surgemq/blob/master/message/connect_test.go
    val typeAndFlags: Int = 0x01 << 4
    val variableHeaderPlusPayload = CompactByteString(
      0, // Length MSB (0)
      4, // Length LSB (4)
      'M', 'Q', 'T', 'T',
      4, // Protocol level 4
      206, // connect flags 11001110, will QoS = 01, cleanSession = true, willRetain = false
      0, // Keep Alive MSB (0)
      10, // Keep Alive LSB (10)
      0, // Client ID MSB (0)
      7, // Client ID LSB (7)
      's', 'u', 'r', 'g', 'e', 'm', 'q',
      0, // Will Topic MSB (0)
      4, // Will Topic LSB (4)
      'w', 'i', 'l', 'l',
      0, // Will Message MSB (0)
      12, // Will Message LSB (12)
      's', 'e', 'n', 'd', ' ', 'm', 'e', ' ', 'h', 'o', 'm', 'e',
      0, // Username ID MSB (0)
      7, // Username ID LSB (7)
      's', 'u', 'r', 'g', 'e', 'm', 'q',
      0, // Password ID MSB (0)
      10, // Password ID LSB (10)
      'v', 'e', 'r', 'y', 's', 'e', 'c', 'r', 'e' // Trailing 't' has been truncated
    )
    val frame = new MqttFrame(typeAndFlags.toByte, variableHeaderPlusPayload)

    ConnectDecoder.decode(frame) match {
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
      case Success(_)  => fail("Should have failed since password has been truncated")
    }
  }

  "ConnectDecoder given a well-formed serialized CONNECT packet with only required fields set" should "correctly decode its contents" in {
    // A telltale case of unabashed robbery: https://github.com/surgemq/surgemq/blob/master/message/connect_test.go
    val typeAndFlags: Int = 0x01 << 4
    val variableHeaderPlusPayload = CompactByteString(
      0, // Length MSB (0)
      5, // Length LSB (4)
      'M', 'Q', 'T', 'T', '4',
      5, // Protocol level 5
      0, // connect flags 00000000, will QoS = 00, cleanSession = false, willRetain = false, willFlag = false
      12, // Keep Alive MSB (12)
      8, // Keep Alive LSB (8)
      0, // Client ID MSB (0)
      7, // Client ID LSB (7)
      's', 'u', 'r', 'g', 'e', 'm', 'q')
    val frame = new MqttFrame(typeAndFlags.toByte, variableHeaderPlusPayload)

    val expectedResult = Connect("MQTT4", ProtocolLevel.UnsupportedProtocolLevel, ClientId("surgemq"), cleanSession = false, 3080, None, None, QoS.AtMostOnce, retainWill = false, None, None)

    ConnectDecoder.decode(frame) match {
      case Failure(_)            => fail("Should not have failed given well-formed input")
      case Success(actualResult) => assert(actualResult == expectedResult)
    }
  }

  "ConnectDecoder given a well-formed serialized CONNECT packet with all optional fields set" should "correctly decode its contents" in {
    // A telltale case of unabashed robbery: https://github.com/surgemq/surgemq/blob/master/message/connect_test.go
    val typeAndFlags: Int = 0x01 << 4
    val variableHeaderPlusPayload = CompactByteString(
      0, // Length MSB (0)
      4, // Length LSB (4)
      'M', 'Q', 'T', 'T',
      4, // Protocol level 4
      206, // connect flags 11001110, will QoS = 01, cleanSession = true, willRetain = false
      0, // Keep Alive MSB (0)
      10, // Keep Alive LSB (10)
      0, // Client ID MSB (0)
      7, // Client ID LSB (7)
      's', 'u', 'r', 'g', 'e', 'm', 'q',
      0, // Will Topic MSB (0)
      4, // Will Topic LSB (4)
      'w', 'i', 'l', 'l',
      0, // Will Message MSB (0)
      12, // Will Message LSB (12)
      's', 'e', 'n', 'd', ' ', 'm', 'e', ' ', 'h', 'o', 'm', 'e',
      0, // Username ID MSB (0)
      7, // Username ID LSB (7)
      's', 'u', 'r', 'g', 'e', 'm', 'q',
      0, // Password ID MSB (0)
      10, // Password ID LSB (10)
      'v', 'e', 'r', 'y', 's', 'e', 'c', 'r', 'e', 't')
    val frame = new MqttFrame(typeAndFlags.toByte, variableHeaderPlusPayload)

    val expectedResult = Connect("MQTT", ProtocolLevel.Level4, ClientId("surgemq"), cleanSession = true, 10, "surgemq", "verysecret", QoS.AtLeastOnce, retainWill = false, Topic("will"), "send me home")

    ConnectDecoder.decode(frame) match {
      case Failure(_)            => fail("Should not have failed given well-formed input")
      case Success(actualResult) => assert(actualResult == expectedResult)
    }
  }
}
