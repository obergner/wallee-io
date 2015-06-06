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

class SubscribeDecoderSpec extends FlatSpec with Matchers {

  "SubscribeDecoder given a serialized SUBSCRIBE packet containing illegal fixed header flags" should "report an error" in {
    // A telltale case of unabashed robbery: https://github.com/surgemq/surgemq/blob/master/message/subscribe_test.go
    val typeAndFlags: Int = 0x80
    val variableHeaderPlusPayload = CompactByteString(
      0, // packet ID MSB (0)
      7, // packet ID LSB (7)
      0, // topic name MSB (0)
      7, // topic name LSB (7)
      's', 'u', 'r', 'g', 'e', 'm', 'q',
      0, // QoS
      0, // topic name MSB (0)
      8, // topic name LSB (8)
      '/', 'a', '/', 'b', '/', '#', '/', 'c',
      1, // QoS
      0, // topic name MSB (0)
      10, // topic name LSB (10)
      '/', 'a', '/', 'b', '/', '#', '/', 'c', 'd', 'd',
      2 // QoS
    )
    val frame = new MqttFrame(typeAndFlags.toByte, variableHeaderPlusPayload)

    SubscribeDecoder.decode(frame) match {
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
      case Success(_)  => fail("Should have failed since SUBSCRIBE packet contains illegal fixed header flags")
    }
  }

  "SubscribeDecoder given a serialized SUBSCRIBE packet containing a topic filter without requested QoS" should "report an error" in {
    // A telltale case of unabashed robbery: https://github.com/surgemq/surgemq/blob/master/message/connect_test.go
    val typeAndFlags: Int = 0x82
    val variableHeaderPlusPayload = CompactByteString(
      0, // packet ID MSB (0)
      7, // packet ID LSB (7)
      0, // topic name MSB (0)
      7, // topic name LSB (7)
      's', 'u', 'r', 'g', 'e', 'm', 'q',
      0, // QoS
      0, // topic name MSB (0)
      8, // topic name LSB (8)
      '/', 'a', '/', 'b', '/', '#', '/', 'c',
      1, // QoS
      0, // topic name MSB (0)
      10, // topic name LSB (10)
      '/', 'a', '/', 'b', '/', '#', '/', 'c', 'd', 'd'
    // Missing requested QoS
    )
    val frame = new MqttFrame(typeAndFlags.toByte, variableHeaderPlusPayload)

    SubscribeDecoder.decode(frame) match {
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
      case Success(_)  => fail("Should have failed since password has been truncated")
    }
  }

  "SubscribeDecoder given a well-formed serialized SUBSCRIBE" should "correctly decode its contents" in {
    // A telltale case of unabashed robbery: https://github.com/surgemq/surgemq/blob/master/message/connect_test.go
    val typeAndFlags: Int = 0x82
    val variableHeaderPlusPayload = CompactByteString(
      0, // packet ID MSB (0)
      7, // packet ID LSB (7)
      0, // topic name MSB (0)
      7, // topic name LSB (7)
      's', 'u', 'r', 'g', 'e', 'm', 'q',
      0, // QoS
      0, // topic name MSB (0)
      8, // topic name LSB (8)
      '/', 'a', '/', 'b', '/', '#', '/', 'c',
      1, // QoS
      0, // topic name MSB (0)
      10, // topic name LSB (10)
      '/', 'a', '/', 'b', '/', '#', '/', 'c', 'd', 'd',
      2 // QoS
    )
    val frame = new MqttFrame(typeAndFlags.toByte, variableHeaderPlusPayload)

    val expectedResult = Subscribe(PacketIdentifier(7), List[Subscription](Subscription("surgemq", QoS.AtMostOnce), Subscription("/a/b/#/c", QoS.AtLeastOnce), Subscription("/a/b/#/cdd", QoS.ExactlyOnce)))

    SubscribeDecoder.decode(frame) match {
      case Failure(_)            => fail("Should not have failed given well-formed input")
      case Success(actualResult) => assert(actualResult == expectedResult)
    }
  }
}
