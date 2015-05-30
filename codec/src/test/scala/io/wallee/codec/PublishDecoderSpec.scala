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

class PublishDecoderSpec extends FlatSpec with Matchers {

  "PublishDecoder given a serialized PUBLISH packet containing a truncated packet identifier" should "report an error" in {
    // A telltale case of unabashed robbery: https://github.com/surgemq/surgemq/blob/master/message/publish_test.go
    val typeAndFlags: Int = 0x30
    val variableHeaderPlusPayload = CompactByteString(
      0, // topic name MSB (0)
      7, // topic name LSB (7)
      's', 'u', 'r', 'g', 'e', 'm', 'q',
      0 // packet ID MSB (0)
    )
    val frame = new MqttFrame(typeAndFlags.toByte, variableHeaderPlusPayload)

    PublishDecoder.decode(frame) match {
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
      case Success(_)  => fail("Should have failed since PUBLISH packet contains illegal fixed header flags")
    }
  }

  "PublishDecoder given a well-formed serialized PUBLISH packet with dup = true, qos = ExactlyOnce and retain = true" should "correctly decode its contents" in {
    // A telltale case of unabashed robbery: https://github.com/surgemq/surgemq/blob/master/message/publish_test.go
    val typeAndFlags: Int = 0x3D
    val variableHeaderPlusPayload = CompactByteString(
      0, // topic name MSB (0)
      7, // topic name LSB (7)
      's', 'u', 'r', 'g', 'e', 'm', 'q',
      0, // packet ID MSB (0)
      7, // packet ID LSB (7)
      's', 'e', 'n', 'd', ' ', 'm', 'e', ' ', 'h', 'o', 'm', 'e'
    )
    val frame = new MqttFrame(typeAndFlags.toByte, variableHeaderPlusPayload)

    val expectedResult = Publish(
      dup = true,
      QoS.ExactlyOnce,
      retain = true,
      Topic("surgemq"),
      PacketIdentifier(7),
      CompactByteString('s', 'e', 'n', 'd', ' ', 'm', 'e', ' ', 'h', 'o', 'm', 'e')
    )

    PublishDecoder.decode(frame) match {
      case Failure(_)            => fail("Should not have failed given well-formed input")
      case Success(actualResult) => assert(actualResult == expectedResult)
    }
  }
}
