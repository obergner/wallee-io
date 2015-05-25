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

package io.wallee.connection.monitor

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.Tcp
import akka.stream.stage.{ Context, PushStage, SyncDirective, TerminationDirective }
import akka.util.ByteString
import io.wallee.connection.logging.TcpConnectionLogging
import io.wallee.shared._

/** [[PushStage]] for logging incoming/outgoing network packets as HEX strings.
 *
 *  @param logPrefix Prefix to prepend to each log message, typically one of "RCVD" and "SEND"
 *  @param level Log level to use when logging network packets
 */
final class LogNetworkPackets(
  protected[this] val connection: Tcp.IncomingConnection,
  logPrefix:                      String, level: Logging.LogLevel
)(protected[this] implicit val system: ActorSystem)
    extends PushStage[ByteString, ByteString] with TcpConnectionLogging {

  override def onPush(elem: ByteString, ctx: Context[ByteString]): SyncDirective = {
    log.log(level, s"$logPrefix: ${byteStringToHex(elem)} (HEX)")
    ctx.push(elem)
  }

  override def onUpstreamFinish(ctx: Context[ByteString]): TerminationDirective = {
    log.info(s"Upstream finished")
    super.onUpstreamFinish(ctx)
  }

  override def onUpstreamFailure(cause: Throwable, ctx: Context[ByteString]): TerminationDirective = {
    log.error(cause, s"Upstream failed: ${cause.getMessage}")
    super.onUpstreamFailure(cause, ctx)
  }
}
