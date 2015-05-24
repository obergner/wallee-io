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

import io.wallee.protocol.QoS
import org.scalatest.{ FlatSpec, Matchers }

class FixedHeaderFlagsSpec extends FlatSpec with Matchers {

  "A FixedHeaderFlags instance" should "correctly recognize that the dup flag is set" in {
    val packetTypeAndFlags = 0x19.toByte
    val underTest = new FixedHeaderFlags(packetTypeAndFlags)

    assert(underTest.dup)
  }

  "A FixedHeaderFlags instance" should "correctly recognize that the dup flag is NOT set" in {
    val packetTypeAndFlags = 0x17.toByte
    val underTest = new FixedHeaderFlags(packetTypeAndFlags)

    assert(!underTest.dup)
  }

  "A FixedHeaderFlags instance" should "correctly recognize QoS AtMostOnce" in {
    val packetTypeAndFlags = 0x29.toByte
    val underTest = new FixedHeaderFlags(packetTypeAndFlags)

    assert(underTest.qos == QoS.AtMostOnce)
  }

  "A FixedHeaderFlags instance" should "correctly recognize QoS AtLeastOnce" in {
    val packetTypeAndFlags = 0x2B.toByte
    val underTest = new FixedHeaderFlags(packetTypeAndFlags)

    assert(underTest.qos == QoS.AtLeastOnce)
  }

  "A FixedHeaderFlags instance" should "correctly recognize QoS ExactlyOnce" in {
    val packetTypeAndFlags = 0x2D.toByte
    val underTest = new FixedHeaderFlags(packetTypeAndFlags)

    assert(underTest.qos == QoS.ExactlyOnce)
  }

  "A FixedHeaderFlags instance" should "correctly recognize that the retain flag is set" in {
    val packetTypeAndFlags = 0x13.toByte
    val underTest = new FixedHeaderFlags(packetTypeAndFlags)

    assert(underTest.retain)
  }

  "A FixedHeaderFlags instance" should "correctly recognize that the retain flag is NOT set" in {
    val packetTypeAndFlags = 0x1E.toByte
    val underTest = new FixedHeaderFlags(packetTypeAndFlags)

    assert(!underTest.retain)
  }
}
