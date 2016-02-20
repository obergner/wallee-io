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

import akka.actor.ActorSystem
import akka.stream.scaladsl.Tcp
import akka.stream.stage._
import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import akka.util.ByteString
import io.wallee.protocol.MalformedMqttPacketException
import io.wallee.shared.logging.TcpConnectionLogging

import scala.util.control.Breaks._

/** Partition incoming byte stream into [[MqttFrame]]s.
 *
 *  ATTENTION: This class is stateful and NOT thread safe.
 */
class FrameDecoderStage(protected[this] val connection: Tcp.IncomingConnection)(protected[this] implicit val system: ActorSystem)
    extends GraphStage[FlowShape[ByteString, MqttFrame]] with TcpConnectionLogging {

  val in = Inlet[ByteString]("FrameDecoderStage.in")

  val out = Outlet[MqttFrame]("FrameDecoderStage.out")

  override def shape: FlowShape[ByteString, MqttFrame] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new FrameDecoderStageLogic(connection, system, shape)
}

@SuppressWarnings(Array("org.brianmckenna.wartremover.warts.Var"))
private class FrameDecoderStageLogic(
  protected[this] val connection: Tcp.IncomingConnection,
  protected[this] val system:     ActorSystem,
  shape:                          FlowShape[ByteString, MqttFrame]
)
    extends GraphStageLogic(shape) with TcpConnectionLogging {

  import FrameDecoderStage._

  val in = shape.in

  val out = shape.out

  private[this] var buffer: ByteString = ByteString.empty

  private[this] val bufferAccess: Buffer = new Buffer {

    override def apply(): ByteString = buffer

    override def apply(buf: ByteString): Unit = buffer = buf
  }

  private[this] var currentState: DecodingState = NoDataConsumed(bufferAccess)

  setHandler(in, new InHandler {
    @throws[Exception](classOf[Exception])
    override def onPush(): Unit = {
      val elem = grab(in)
      log.debug(s"DECODE FRAME: $elem (cached:$buffer)")
      buffer ++= elem
      emitFrameOrPull()
    }
  })

  setHandler(out, new OutHandler {
    @throws[Exception](classOf[Exception])
    override def onPull(): Unit = emitFrameOrPull()
  })

  private def emitFrameOrPull(): Unit = {
    if (buffer.isEmpty) {
      pull(in)
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
          log.debug(s"DECODED FRAME: $frame")
          push(out, frame)
        case IllegalRemainingLength(_) =>
          currentState = NoDataConsumed(bufferAccess)
          val ex: MalformedMqttPacketException = new MalformedMqttPacketException("Illegal remaining length field in MQTT packet")
          log.error(ex, s"Failed to decode MQTT frame: ${ex.getMessage}")
          fail(out, ex)
        case _ => pull(in)
      }
    }
  }
}

object FrameDecoderStage {

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
        case Right(RemainingLengthDecoder.Result(remainingLength, remainder)) if remainder.isEmpty => // Packet has no payload, e.g. PINGREQ
          buffer(remainder)
          MqttFrameDecoded(buffer, new MqttFrame(firstHeaderByte, remainder))
        case Right(RemainingLengthDecoder.Result(remainingLength, remainder)) =>
          buffer(remainder)
          ConsumingVariableHeaderAndPayload(buffer, firstHeaderByte, remainingLength)
      }
    }
  }

  final case class IllegalRemainingLength(buffer: Buffer) extends DecodingState(buffer) {

    override def onNewInput(): DecodingState = throw new UnsupportedOperationException("IllegalRemainingLength is a terminal state")
  }

  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.Var"))
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
