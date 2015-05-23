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

import akka.util.ByteString

/**
 * Decodes an MQTT packet's `remaining length`.
 *
 * ATTENTION: This class is stateful and NOT thread safe.
 *
 * @see http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718023
 */
class RemainingLengthDecoder
  extends (ByteString => Either[RemainingLengthDecoder.State, RemainingLengthDecoder.Result]) {

  import RemainingLengthDecoder._

  var current: Int = 0

  var exponent: Int = 0

  override def apply(byteString: ByteString): Either[RemainingLengthDecoder.State, RemainingLengthDecoder.Result] = {
    var moreInputNeeded = true
    val remainder = byteString.dropWhile(b => {
      moreInputNeeded = update(b)
      moreInputNeeded && (exponent < MaxDigits)
    })

    if (moreInputNeeded) Left(if (exponent >= MaxDigits) MaxRemainingLengthExceeded else MoreInputNeeded) else Right(Result(current, remainder.drop(1)))
  }

  private def update(input: Byte): Boolean = {
    current += (input & ValueBitsMask) * scala.math.pow(128, exponent).floor.toInt
    exponent += 1
    moreBytesNeeded(input)
  }

  private def moreBytesNeeded(input: Byte): Boolean = (input & ContinuationBitMask) == ContinuationBitMask

  def reset(): Unit = {
    current = 0
    exponent = 0
  }
}

object RemainingLengthDecoder {

  final val ContinuationBitMask: Byte = 0x80.toByte

  final val ValueBitsMask: Byte = 0x7F.toByte

  final val MaxDigits: Int = 4

  final val MaxRemainingLength: Int = 127 * (128 * 128 * 128) + 127 * (128 * 128) + 127 * 128 + 127

  sealed trait State

  object MoreInputNeeded extends State

  object MaxRemainingLengthExceeded extends State

  final case class Result(remainingLength: Int, remainder: ByteString)

}
