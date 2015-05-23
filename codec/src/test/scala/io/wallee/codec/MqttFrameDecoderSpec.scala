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
import akka.util.{ByteStringBuilder, CompactByteString}
import org.scalatest.{FlatSpec, Matchers}

class MqttFrameDecoderSpec extends FlatSpec with Matchers {

  implicit val as = ActorSystem()
  implicit val fm = ActorFlowMaterializer()

  "A new MqttFrameDecoder when given a single serialized packet in a single chunk as input" should "correctly decode that frame" in {
    val chunk = CompactByteString(0x10, 0x04, 0x01, 0x01, 0x01, 0x01)
    val expectedDecodedFrame = new MqttFrame(0x10, chunk.drop(2))

    Source.single(chunk)
      .transform(() => new MqttFrameDecoder)
      .runWith(TestSink.probe[MqttFrame])
      .request(1)
      .expectNext(expectedDecodedFrame)
      .expectComplete()
  }

  "A new MqttFrameDecoder when given a single serialized packet in two chunks as input" should "correctly decode that frame" in {
    val firstChunk = CompactByteString(0x21, 0x04, 0x01)
    val secondChunk = CompactByteString(0x02, 0x03, 0x04)
    val expectedDecodedFrame = new MqttFrame(0x21, CompactByteString(0x01, 0x02, 0x03, 0x04))

    Source(List(firstChunk, secondChunk))
      .transform(() => new MqttFrameDecoder)
      .runWith(TestSink.probe[MqttFrame])
      .request(1)
      .expectNext(expectedDecodedFrame)
      .expectComplete()
  }

  "A new MqttFrameDecoder when given a single serialized packet in three chunks as input" should "correctly decode that frame" in {
    val firstChunk = CompactByteString(0x31, 0x06, 0x01)
    val secondChunk = CompactByteString(0x02)
    val thirdChunk = CompactByteString(0x03, 0x04, 0x05, 0x06)
    val expectedDecodedFrame = new MqttFrame(0x31, CompactByteString(0x01, 0x02, 0x03, 0x04, 0x05, 0x06))

    Source(List(firstChunk, secondChunk, thirdChunk))
      .transform(() => new MqttFrameDecoder)
      .runWith(TestSink.probe[MqttFrame])
      .request(1)
      .expectNext(expectedDecodedFrame)
      .expectComplete()
  }

  "A new MqttFrameDecoder when given two serialized packets in three chunks as input" should "correctly decode two frames" in {
    val firstChunk = CompactByteString(0x42, 0x05, 0x01)
    val secondChunk = CompactByteString(0x02, 0x03, 0x04, 0x05, 0x21)
    val thirdChunk = CompactByteString(0x03, 0x01, 0x02, 0x03, 0x00, 0x00, 0x00)
    val expectedFirstDecodedFrame = new MqttFrame(0x42, CompactByteString(0x01, 0x02, 0x03, 0x04, 0x05))
    val expectedSecondDecodedFrame = new MqttFrame(0x21, CompactByteString(0x01, 0x02, 0x03))

    Source(List(firstChunk, secondChunk, thirdChunk))
      .transform(() => new MqttFrameDecoder)
      .runWith(TestSink.probe[MqttFrame])
      .request(2)
      .expectNext(expectedFirstDecodedFrame, expectedSecondDecodedFrame)
      .expectComplete()
  }

  "A new MqttFrameDecoder when given three serialized packets in five chunks as input" should "correctly decode three frames" in {
    val thirdRemainingLength = 1 + 42 * 128
    val firstChunk = CompactByteString(0x42, 0x05, 0x01)
    val secondChunk = CompactByteString(0x02, 0x03, 0x04, 0x05, 0x21)
    val thirdChunk = CompactByteString(0x03, 0x01, 0x02, 0x03, 0x31, 0x81)
    val fourthChunk = CompactByteString(0x2A, 0x01, 0x01)

    val builder = new ByteStringBuilder
    for (_ <- 1 to (thirdRemainingLength - 1)) {
      builder.putByte(0x01)
    }
    val fifthChunk = builder.result().compact

    val builder2 = new ByteStringBuilder
    for (_ <- 1 to thirdRemainingLength) {
      builder2.putByte(0x01)
    }
    val thirdFramePayload = builder2.result().compact

    val expectedFirstDecodedFrame = new MqttFrame(0x42, CompactByteString(0x01, 0x02, 0x03, 0x04, 0x05))
    val expectedSecondDecodedFrame = new MqttFrame(0x21, CompactByteString(0x01, 0x02, 0x03))
    val expectedThirdDecodedFrame = new MqttFrame(0x31, thirdFramePayload)

    Source(List(firstChunk, secondChunk, thirdChunk, fourthChunk, fifthChunk))
      .transform(() => new MqttFrameDecoder)
      .runWith(TestSink.probe[MqttFrame])
      .request(3)
      .expectNext(expectedFirstDecodedFrame, expectedSecondDecodedFrame, expectedThirdDecodedFrame)
      .expectComplete()
  }
}
