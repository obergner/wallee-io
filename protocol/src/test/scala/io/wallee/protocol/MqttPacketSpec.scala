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

package io.wallee.protocol

import org.scalatest.{ FlatSpec, Matchers }

class MqttPacketSpec extends FlatSpec with Matchers {

  "An MqttPacket with remainingLength < 128" should "report correct lengthInBytes" in {
    val remLength = 127
    val underTest = new MqttPacket {
      override protected def remainingLength: Int = remLength
    }

    assert(underTest.lengthInBytes == remLength + 2)
  }

  "An MqttPacket with remainingLength == 128" should "report correct lengthInBytes" in {
    val remLength = 128
    val underTest = new MqttPacket {
      override protected def remainingLength: Int = remLength
    }

    assert(underTest.lengthInBytes == remLength + 3)
  }

  "An MqttPacket with 128 <= remainingLength < 16384" should "report correct lengthInBytes" in {
    val remLength = 16383
    val underTest = new MqttPacket {
      override protected def remainingLength: Int = remLength
    }

    assert(underTest.lengthInBytes == remLength + 3)
  }

  "An MqttPacket with remainingLength == 16384" should "report correct lengthInBytes" in {
    val remLength = 16384
    val underTest = new MqttPacket {
      override protected def remainingLength: Int = remLength
    }

    assert(underTest.lengthInBytes == remLength + 4)
  }

  "An MqttPacket with 16384 <= remainingLength < 2097152" should "report correct lengthInBytes" in {
    val remLength = 2097151
    val underTest = new MqttPacket {
      override protected def remainingLength: Int = remLength
    }

    assert(underTest.lengthInBytes == remLength + 4)
  }

  "An MqttPacket with remainingLength == 2097152" should "report correct lengthInBytes" in {
    val remLength = 2097152
    val underTest = new MqttPacket {
      override protected def remainingLength: Int = remLength
    }

    assert(underTest.lengthInBytes == remLength + 5)
  }

  "An MqttPacket with 2097152 <= remainingLength < 268435456" should "report correct lengthInBytes" in {
    val remLength = 268435455
    val underTest = new MqttPacket {
      override protected def remainingLength: Int = remLength
    }

    assert(underTest.lengthInBytes == remLength + 5)
  }

  "An MqttPacket with remainingLength >= 268435456" should "throw IllegalArgumentException upon creation" in {
    val remLength = 268435456

    intercept[MalformedMqttPacketException] {
      new MqttPacket {
        override protected def remainingLength: Int = remLength
      }
    }
  }
}
