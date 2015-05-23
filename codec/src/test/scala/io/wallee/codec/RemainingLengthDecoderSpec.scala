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

import akka.util.{ByteString, CompactByteString}
import io.wallee.codec.RemainingLengthDecoder.{MaxRemainingLengthExceeded, MoreInputNeeded}
import org.scalatest.{FlatSpec, Matchers}

class RemainingLengthDecoderSpec extends FlatSpec with Matchers {

  "A new RemainingLengthDecoder instance" should "return MoreInputNeeded if passed an empty chunk" in {
    val inputChunk: ByteString = CompactByteString()

    val underTest = new RemainingLengthDecoder
    underTest(inputChunk) match {
      case Left(state) => assert(state == MoreInputNeeded)
      case Right(result) => fail("RemainingLengthDecoder incorrectly returned Right(result)")
    }
  }

  "A new RemainingLengthDecoder instance" should "correctly decode a valid one-byte remaining length passed in a single chunk" in {
    val expectedRemainingLength: Int = 127
    val inputChunk: ByteString = CompactByteString(0x7F.toByte, 0xFF.toByte)

    val underTest = new RemainingLengthDecoder
    underTest(inputChunk) match {
      case Left(state) => fail("RemainingLengthDecoder incorrectly returned Left(state)")
      case Right(result) =>
        assert(result.remainingLength == expectedRemainingLength, "RemainingLengthDecoder computed incorrect remaining length")
        assert(result.remainder(0) == inputChunk(1), "RemainingLengthDecoder did not consume correct number of bytes in input")
    }
  }

  "A new RemainingLengthDecoder instance" should "correctly decode a valid two-bytes remaining length passed in a single chunk" in {
    val expectedRemainingLength: Int = 2 + 111 * 128
    val inputChunk: ByteString = CompactByteString(0x82.toByte, 0x6F.toByte, 0x01.toByte)

    val underTest = new RemainingLengthDecoder
    underTest(inputChunk) match {
      case Left(state) => fail("RemainingLengthDecoder incorrectly returned Left(state)")
      case Right(result) =>
        assert(result.remainingLength == expectedRemainingLength, "RemainingLengthDecoder computed incorrect remaining length")
        assert(result.remainder(0) == inputChunk(2), "RemainingLengthDecoder did not consume correct number of bytes in input")
    }
  }

  "A new RemainingLengthDecoder instance" should "correctly decode a valid three-bytes remaining length passed in a single chunk" in {
    val expectedRemainingLength: Int = 15 + 127 * 128 + 81 * (128 * 128)
    val inputChunk: ByteString = CompactByteString(0x8F.toByte, 0xFF.toByte, 0x51.toByte, 0x6F.toByte)

    val underTest = new RemainingLengthDecoder
    underTest(inputChunk) match {
      case Left(state) => fail("RemainingLengthDecoder incorrectly returned Left(state)")
      case Right(result) =>
        assert(result.remainingLength == expectedRemainingLength, "RemainingLengthDecoder computed incorrect remaining length")
        assert(result.remainder(0) == inputChunk(3), "RemainingLengthDecoder did not consume correct number of bytes in input")
    }
  }

  "A new RemainingLengthDecoder instance" should "correctly decode a valid four-bytes remaining length passed in a single chunk" in {
    val expectedRemainingLength: Int = 76 + 58 * 128 + 81 * (128 * 128) + 17 * (128 * 128 * 128)
    val inputChunk: ByteString = CompactByteString(0xCC.toByte, 0xBA.toByte, 0xD1.toByte, 0x11.toByte, 0x6F.toByte)

    val underTest = new RemainingLengthDecoder
    underTest(inputChunk) match {
      case Left(state) => fail("RemainingLengthDecoder incorrectly returned Left(state)")
      case Right(result) =>
        assert(result.remainingLength == expectedRemainingLength, "RemainingLengthDecoder computed incorrect remaining length")
        assert(result.remainder(0) == inputChunk(4), "RemainingLengthDecoder did not consume correct number of bytes in input")
    }
  }

  "A new RemainingLengthDecoder instance" should "correctly report an invalid remaining length passed in a single chunk" in {
    val inputChunk: ByteString = CompactByteString(0xCC.toByte, 0xBA.toByte, 0xD1.toByte, 0x81.toByte, 0x00.toByte, 0x6F.toByte)

    val underTest = new RemainingLengthDecoder
    underTest(inputChunk) match {
      case Left(state) => assert(state == MaxRemainingLengthExceeded)
      case Right(result) => fail("RemainingLengthDecoder incorrectly returned Right(result)")
    }
  }

  "A new RemainingLengthDecoder instance" should "correctly decode a valid four-bytes remaining length passed in two chunks" in {
    val expectedRemainingLength: Int = 76 + 58 * 128 + 81 * (128 * 128) + 17 * (128 * 128 * 128)
    val firstInputChunk: ByteString = CompactByteString(0xCC.toByte, 0xBA.toByte)
    val secondInputChunk: ByteString = CompactByteString(0xD1.toByte, 0x11.toByte, 0x6F.toByte)

    val underTest = new RemainingLengthDecoder
    underTest(firstInputChunk) match {
      case Left(state) => assert(state == MoreInputNeeded)
      case Right(result) => fail("RemainingLengthDecoder incorrectly returned Right(result)")
    }

    underTest(secondInputChunk) match {
      case Left(state) => fail("RemainingLengthDecoder incorrectly returned Left(state)")
      case Right(result) =>
        assert(result.remainingLength == expectedRemainingLength, "RemainingLengthDecoder computed incorrect remaining length")
        assert(result.remainder(0) == secondInputChunk(2), "RemainingLengthDecoder did not consume correct number of bytes in input")
    }
  }

  "A new RemainingLengthDecoder instance" should "correctly report an invalid remaining length passed in two chunks" in {
    val firstInputChunk: ByteString = CompactByteString(0xCC.toByte, 0xBA.toByte)
    val secondInputChunk: ByteString = CompactByteString(0xD1.toByte, 0xA5.toByte, 0x99.toByte, 0x6F.toByte)

    val underTest = new RemainingLengthDecoder
    underTest(firstInputChunk) match {
      case Left(state) => assert(state == MoreInputNeeded)
      case Right(result) => fail("RemainingLengthDecoder incorrectly returned Right(result)")
    }

    underTest(secondInputChunk) match {
      case Left(state) => assert(state == MaxRemainingLengthExceeded)
      case Right(result) => fail("RemainingLengthDecoder incorrectly returned Right(result)")
    }
  }
}
