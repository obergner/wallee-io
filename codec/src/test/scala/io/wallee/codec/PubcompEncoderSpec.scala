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
import io.wallee.protocol.{ PacketIdentifier, Pubcomp }
import org.scalatest.{ FlatSpec, Matchers }

import scala.util.{ Failure, Success }

class PubcompEncoderSpec extends FlatSpec with Matchers {

  "PubcompEncoder when given a Pubcomp" should "correctly encode it" in {
    val expectedResult = CompactByteString(0x70, 0x02, 0xA1, 0x3F)
    val pingResp = Pubcomp(PacketIdentifier(41279))

    PubcompEncoder.encode(pingResp) match {
      case Success(actualResult) => assert(actualResult == expectedResult)
      case Failure(_)            => fail("Should not have reported an error for well-formed input")
    }
  }
}
