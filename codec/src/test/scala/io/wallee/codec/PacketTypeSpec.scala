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

import org.scalatest.{ FlatSpec, Matchers }

class PacketTypeSpec extends FlatSpec with Matchers {

  "PacketType.parse" should "correctly parse CONNECT in a header byte" in {
    val firstHeaderByte: Byte = 0x1F

    assert(PacketType.parse(firstHeaderByte) == PacketType.Connect)
  }

  "PacketType.parse" should "correctly parse CONNACK in a header byte" in {
    val firstHeaderByte: Byte = 0x2C

    assert(PacketType.parse(firstHeaderByte) == PacketType.Connack)
  }

  "PacketType.parse" should "correctly parse PUBLISH in a header byte" in {
    val firstHeaderByte: Byte = 0x31

    assert(PacketType.parse(firstHeaderByte) == PacketType.Publish)
  }

  "PacketType.parse" should "correctly parse PUBACK in a header byte" in {
    val firstHeaderByte: Byte = 0x42

    assert(PacketType.parse(firstHeaderByte) == PacketType.Puback)
  }
}
