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
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{ Flow, Source, Tcp }
import akka.stream.testkit.scaladsl.TestSink
import akka.util.{ ByteString, CompactByteString }
import io.wallee.protocol.{ Connack, ConnectReturnCode, MqttPacket, PingResp }
import org.scalatest.{ FlatSpec, Matchers }

class EncoderStageSpec extends FlatSpec with Matchers {

  implicit val as = ActorSystem()

  implicit val fm = ActorFlowMaterializer()

  val connection = Tcp.IncomingConnection(new InetSocketAddress(111), new InetSocketAddress(222), Flow[ByteString])

  "An EncoderStage given a well-formed CONNACK packet as input" should "propagate correctly encoded packet" in {
    val expectedResult = CompactByteString(0x20, 0x02, 0x01, 0x04)
    val connack = Connack(sessionPresent = true, ConnectReturnCode.BadUsernameOrPassword)

    Source.single[MqttPacket](connack)
      .transform(() => new EncoderStage(connection))
      .runWith(TestSink.probe[ByteString])
      .request(1)
      .expectNext(expectedResult)
      .expectComplete()
  }

  "An EncoderStage given a well-formed PINGRESP packet as input" should "propagate correctly encoded packet" in {
    val expectedResult = CompactByteString(0xD0, 0x00)
    val pingResp = PingResp()

    Source.single[MqttPacket](pingResp)
      .transform(() => new EncoderStage(connection))
      .runWith(TestSink.probe[ByteString])
      .request(1)
      .expectNext(expectedResult)
      .expectComplete()
  }
}
