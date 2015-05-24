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

import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.util.CompactByteString
import io.wallee.protocol._
import org.scalatest.{ FlatSpec, Matchers }

class DecoderStageSpec extends FlatSpec with Matchers {

  implicit val as = ActorSystem()
  implicit val fm = ActorFlowMaterializer()

  "DecoderStage when processing a well-formed MqttFrame of type CONNECT" should "transform it into a Connect packet" in {
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
      'v', 'e', 'r', 'y', 's', 'e', 'c', 'r', 'e', 't'
    )
    val frame = new MqttFrame(typeAndFlags.toByte, variableHeaderPlusPayload)

    val expectedResult = Connect("MQTT", ProtocolLevel.Level4, ClientId("surgemq"), cleanSession = true, 10, "surgemq", "verysecret", QoS.AtLeastOnce, retainWill = false, Topic("will"), "send me home")

    Source.single[MqttFrame](frame)
      .transform(() => new DecoderStage)
      .runWith(TestSink.probe[MqttPacket])
      .request(1)
      .expectNext(expectedResult)
      .expectComplete()
  }

  "DecoderStage when processing an MqttFrame of unsupported type" should "propagate an IllegalArgumentException downstream" in {
    // A telltale case of unabashed robbery: https://github.com/surgemq/surgemq/blob/master/message/connect_test.go
    val typeAndFlags: Int = 0x20
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
      'v', 'e', 'r', 'y', 's', 'e', 'c', 'r', 'e', 't'
    )
    val frame = new MqttFrame(typeAndFlags.toByte, variableHeaderPlusPayload)

    val error = Source.single[MqttFrame](frame)
      .transform(() => new DecoderStage)
      .runWith(TestSink.probe[MqttPacket])
      .request(1)
      .expectError()

    assert(error.isInstanceOf[IllegalArgumentException])
  }

  "DecoderStage when processing a malformed MqttFrame" should "propagate a MalformedMqttPacketException downstream" in {
    // A telltale case of unabashed robbery: https://github.com/surgemq/surgemq/blob/master/message/connect_test.go
    val typeAndFlags: Int = 0x10
    val variableHeaderPlusPayload = CompactByteString(
      0, // Length MSB (0)
      4, // Length LSB (4)
      'M', 'Q', 'T', // Missing 'T' !
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
      'v', 'e', 'r', 'y', 's', 'e', 'c', 'r', 'e', 't'
    )
    val frame = new MqttFrame(typeAndFlags.toByte, variableHeaderPlusPayload)

    val error = Source.single[MqttFrame](frame)
      .transform(() => new DecoderStage)
      .runWith(TestSink.probe[MqttPacket])
      .request(1)
      .expectError()

    assert(error.isInstanceOf[MalformedMqttPacketException])
  }
}
