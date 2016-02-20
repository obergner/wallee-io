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

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Source, Tcp}
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.{ByteString, ByteStringBuilder, CompactByteString}
import io.wallee.protocol.MalformedMqttPacketException
import org.scalatest.{FlatSpec, Matchers}

class FrameDecoderStageSpec extends FlatSpec with Matchers {

  final implicit val as = ActorSystem()

  final implicit val fm = ActorMaterializer(ActorMaterializerSettings(as))

  val connection = Tcp.IncomingConnection(new InetSocketAddress(111), new InetSocketAddress(222), Flow[ByteString])

  "A new FrameDecoderStage when given a single serialized packet in a single chunk as input" should "correctly decode that frame" in {
    val chunk = CompactByteString(0x10, 0x04, 0x01, 0x01, 0x01, 0x01)
    val expectedDecodedFrame = new MqttFrame(0x10, chunk.drop(2))

    Source.single(chunk)
      .via(new FrameDecoderStage(connection))
      .runWith(TestSink.probe[MqttFrame])
      .request(1)
      .expectNext(expectedDecodedFrame)
      .expectComplete()
  }

  "A new FrameDecoderStage when given a single serialized packet in two chunks as input" should "correctly decode that frame" in {
    val firstChunk = CompactByteString(0x21, 0x04, 0x01)
    val secondChunk = CompactByteString(0x02, 0x03, 0x04)
    val expectedDecodedFrame = new MqttFrame(0x21, CompactByteString(0x01, 0x02, 0x03, 0x04))

    Source(List(firstChunk, secondChunk))
      .via(new FrameDecoderStage(connection))
      .runWith(TestSink.probe[MqttFrame])
      .request(1)
      .expectNext(expectedDecodedFrame)
      .expectComplete()
  }

  "A new FrameDecoderStage when given a single serialized packet in three chunks as input" should "correctly decode that frame" in {
    val firstChunk = CompactByteString(0x31, 0x06, 0x01)
    val secondChunk = CompactByteString(0x02)
    val thirdChunk = CompactByteString(0x03, 0x04, 0x05, 0x06)
    val expectedDecodedFrame = new MqttFrame(0x31, CompactByteString(0x01, 0x02, 0x03, 0x04, 0x05, 0x06))

    Source(List(firstChunk, secondChunk, thirdChunk))
      .via(new FrameDecoderStage(connection))
      .runWith(TestSink.probe[MqttFrame])
      .request(1)
      .expectNext(expectedDecodedFrame)
      .expectComplete()
  }

  "A new FrameDecoderStage when given two serialized packets in three chunks as input" should "correctly decode two frames" in {
    val firstChunk = CompactByteString(0x42, 0x05, 0x01)
    val secondChunk = CompactByteString(0x02, 0x03, 0x04, 0x05, 0x21)
    val thirdChunk = CompactByteString(0x03, 0x01, 0x02, 0x03, 0x00, 0x00, 0x00)
    val expectedFirstDecodedFrame = new MqttFrame(0x42, CompactByteString(0x01, 0x02, 0x03, 0x04, 0x05))
    val expectedSecondDecodedFrame = new MqttFrame(0x21, CompactByteString(0x01, 0x02, 0x03))

    Source(List(firstChunk, secondChunk, thirdChunk))
      .via(new FrameDecoderStage(connection))
      .runWith(TestSink.probe[MqttFrame])
      .request(2)
      .expectNext(expectedFirstDecodedFrame, expectedSecondDecodedFrame)
      .expectComplete()
  }

  "A new FrameDecoderStage when given three serialized packets in five chunks as input" should "correctly decode three frames" in {
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
      .via(new FrameDecoderStage(connection))
      .runWith(TestSink.probe[MqttFrame])
      .request(3)
      .expectNext(expectedFirstDecodedFrame, expectedSecondDecodedFrame, expectedThirdDecodedFrame)
      .expectComplete()
  }

  "A new FrameDecoderStage when given a single serialized packet with illegal remaining length in three chunks as input" should
    "propagate a MalformedMqttPacketException downstream" in {
      val firstChunk = CompactByteString(0x31, 0x81, 0xA0)
      val secondChunk = CompactByteString(0x80, 0x91)
      val thirdChunk = CompactByteString(0x01, 0x02, 0x03, 0x04, 0x05)

      val error = Source(List(firstChunk, secondChunk, thirdChunk))
        .via(new FrameDecoderStage(connection))
        .runWith(TestSink.probe[MqttFrame])
        .request(1)
        .expectError()

      assert(error.isInstanceOf[MalformedMqttPacketException])
    }

  "A new FrameDecoderStage when given a serialized CONNECT followed by a PINGREQ as input" should "correctly decode two frames" in {
    val connectTypeAndFlags: Int = 0x01 << 4
    val connectPayload = CompactByteString(
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
    val firstChunk = CompactByteString(connectTypeAndFlags, 0x3C) ++ connectPayload

    val pingReqTypeAndFlags: Int = 0x0C << 4
    val secondChunk = CompactByteString(pingReqTypeAndFlags.toByte, 0x00)

    val expectedDecodedConnectFrame = new MqttFrame(connectTypeAndFlags.toByte, connectPayload)
    val expectedDecodedPingReqFrame = new MqttFrame(pingReqTypeAndFlags.toByte, ByteString.empty)

    Source(List(firstChunk, secondChunk))
      .log("frame")
      .via(new FrameDecoderStage(connection))
      .runWith(TestSink.probe[MqttFrame])
      .request(2)
      .expectNext(expectedDecodedConnectFrame, expectedDecodedPingReqFrame)
      .expectComplete()
  }
}
