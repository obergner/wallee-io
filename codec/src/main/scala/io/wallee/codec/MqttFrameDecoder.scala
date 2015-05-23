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

import akka.stream.stage.{ Context, PushPullStage, SyncDirective }
import akka.util.ByteString
import io.wallee.protocol.MalformedMqttPacketException

import scala.util.control.Breaks._

/** Partition incoming byte stream into [[MqttFrame]]s.
 *
 *  ATTENTION: This class is stateful and NOT thread safe.
 */
class MqttFrameDecoder extends PushPullStage[ByteString, MqttFrame] {

  import MqttFrameDecoder._

  private[this] var buffer: ByteString = ByteString.empty

  private[this] val bufferAccess: Buffer = new Buffer {

    override def apply(): ByteString = buffer

    override def apply(buf: ByteString): Unit = buffer = buf
  }

  private[this] var currentState: DecodingState = NoDataConsumed(bufferAccess)

  override def onPush(elem: ByteString, ctx: Context[MqttFrame]): SyncDirective = {
    buffer ++= elem
    emitFrameOrPull(ctx)
  }

  private def emitFrameOrPull(ctx: Context[MqttFrame]): SyncDirective = {
    if (buffer.isEmpty) {
      ctx.pull()
    } else {
      breakable {
        while (buffer.nonEmpty) {
          currentState = currentState.onNewInput()
          currentState match {
            case MqttFrameDecoded(_, _) | IllegalRemainingLength(_) => break()
            case _                                                  =>
          }
        }
      }

      currentState match {
        case MqttFrameDecoded(_, frame) =>
          currentState = NoDataConsumed(bufferAccess)
          ctx.push(frame)
        case IllegalRemainingLength(_) =>
          currentState = NoDataConsumed(bufferAccess)
          ctx.fail(new MalformedMqttPacketException("Illegal remaining length field in MQTT packet")) // FIXME: We should rather close this connection
        case _ => ctx.pull()
      }
    }
  }

  override def onPull(ctx: Context[MqttFrame]): SyncDirective = emitFrameOrPull(ctx)

}

object MqttFrameDecoder {

  sealed trait Buffer {

    def apply(): ByteString

    def apply(buf: ByteString): Unit
  }

  /*
   * State machine
   */

  sealed abstract class DecodingState(buffer: Buffer) {

    def onNewInput(): DecodingState
  }

  final case class NoDataConsumed(buffer: Buffer) extends DecodingState(buffer) {
    override def onNewInput(): DecodingState = {
      if (buffer().isEmpty) {
        this
      } else {
        val firstHeaderByte = buffer().head
        buffer(buffer().tail)
        DecodingFixedHeader(buffer, firstHeaderByte)
      }
    }
  }

  final case class DecodingFixedHeader(buffer: Buffer, firstHeaderByte: Byte) extends DecodingState(buffer) {

    private[this] val remainingLengthDecoder = new RemainingLengthDecoder

    override def onNewInput(): DecodingState = {
      remainingLengthDecoder(buffer()) match {
        case Left(state) if state == RemainingLengthDecoder.MoreInputNeeded =>
          buffer(ByteString.empty) // We know that our RemainingLengthDecoder has completely consumed our buffer
          this
        case Left(state) if state == RemainingLengthDecoder.MaxRemainingLengthExceeded =>
          buffer(ByteString.empty) // This is potentially dangerous: we don't know exactly whether it's safe to throw away all bytes
          IllegalRemainingLength(buffer)
        case Right(RemainingLengthDecoder.Result(remainingLength, remainder)) =>
          buffer(remainder)
          ConsumingVariableHeaderAndPayload(buffer, firstHeaderByte, remainingLength)
      }
    }
  }

  final case class IllegalRemainingLength(buffer: Buffer) extends DecodingState(buffer) {

    override def onNewInput(): DecodingState = throw new UnsupportedOperationException("IllegalRemainingLength is a terminal state")
  }

  final case class ConsumingVariableHeaderAndPayload(buffer: Buffer, firstHeaderByte: Byte, remainingLength: Int)
      extends DecodingState(buffer) {

    private[this] var variableHeaderAndPayload = ByteString.empty

    override def onNewInput(): DecodingState = {
      if (variableHeaderAndPayload.size + buffer().size < remainingLength) {
        variableHeaderAndPayload = variableHeaderAndPayload ++ buffer()
        buffer(ByteString.empty)
        this
      } else {
        val (frameBufRest, rest) = buffer().splitAt(remainingLength - variableHeaderAndPayload.size)
        val frameBuf = variableHeaderAndPayload ++ frameBufRest
        variableHeaderAndPayload = ByteString.empty // Just being paranoid ...
        buffer(rest)
        MqttFrameDecoded(buffer, new MqttFrame(firstHeaderByte, frameBuf))
      }
    }
  }

  final case class MqttFrameDecoded(buffer: Buffer, frame: MqttFrame) extends DecodingState(buffer) {
    override def onNewInput(): DecodingState = throw new UnsupportedOperationException("MqttFrameDecoded is a terminal state")
  }

}
