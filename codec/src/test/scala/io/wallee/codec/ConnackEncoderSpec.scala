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
import io.wallee.protocol.{ Connack, ConnectReturnCode }
import org.scalatest.{ FlatSpec, Matchers }

import scala.util.{ Failure, Success }

class ConnackEncoderSpec extends FlatSpec with Matchers {

  "ConnackEncoder when given a CONNACK packet with session present flag set" should "correctly encode it" in {
    val expectedResult = CompactByteString(0x20, 0x02, 0x01, 0x04)
    val connack = Connack(sessionPresent = true, ConnectReturnCode.BadUsernameOrPassword)

    ConnackEncoder.encode(connack) match {
      case Success(actualResult) => assert(actualResult == expectedResult)
      case Failure(_)            => fail("Should not have reported an error for well-formed input")
    }
  }

  "ConnackEncoder when given a CONNACK packet with session present flag NOT set" should "correctly encode it" in {
    val expectedResult = CompactByteString(0x20, 0x02, 0x00, 0x05)
    val connack = Connack(sessionPresent = false, ConnectReturnCode.NotAuthorized)

    ConnackEncoder.encode(connack) match {
      case Success(actualResult) => assert(actualResult == expectedResult)
      case Failure(_)            => fail("Should not have reported an error for well-formed input")
    }
  }
}
