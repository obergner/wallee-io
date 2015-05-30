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

class PubcompDecoderSpec extends FlatSpec with Matchers {

  "PubcompDecoder given a serialized PUBCOMP packet containing illegal fixed header flags" should "report an error" in {
    val typeAndFlags: Int = 0x71
    val variableHeaderPlusPayload = CompactByteString(0xA1, 0x3F)
    val frame = new MqttFrame(typeAndFlags.toByte, variableHeaderPlusPayload)

    PubcompDecoder.decode(frame) match {
      case Failure(ex) => assert(ex.isInstanceOf[MalformedMqttPacketException])
      case Success(_)  => fail("Should have failed since PUBCOMP packet contains illegal fixed header flags")
    }
  }

  "PubcompDecoder given a well-formed serialized PUBCOMP packet" should "correctly decode it" in {
    val typeAndFlags: Int = 0x70
    val variableHeaderPlusPayload = CompactByteString(0xA1, 0x3F)
    val frame = new MqttFrame(typeAndFlags.toByte, variableHeaderPlusPayload)

    PubcompDecoder.decode(frame) match {
      case Failure(_)      => fail("Should have correctly decoded a well-formed PUBCOMP packet")
      case Success(packet) => assert(packet.equals(Pubcomp(PacketIdentifier(41279))))
    }
  }
}
