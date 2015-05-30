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

package io.wallee.connection.error

import akka.actor.ActorSystem
import akka.stream.scaladsl.Tcp
import akka.stream.stage.{ Context, PushStage, SyncDirective, TerminationDirective }
import io.wallee.protocol.MqttPacket
import io.wallee.shared.logging.TcpConnectionLogging

/** [[PushStage]] for logging decoding errors.
 *
 *  @param connection [[Tcp.IncomingConnection connection]] this [[PushStage stage]] is attached to
 *  @param system [[ActorSystem]] we are part of
 */
class DecodingErrorLogger(protected[this] val connection: Tcp.IncomingConnection)(protected[this] implicit val system: ActorSystem)
    extends PushStage[MqttPacket, MqttPacket] with TcpConnectionLogging {

  override def onPush(elem: MqttPacket, ctx: Context[MqttPacket]): SyncDirective = {
    ctx.push(elem)
  }

  override def onUpstreamFailure(cause: Throwable, ctx: Context[MqttPacket]): TerminationDirective = {
    log.error(cause, s"Error in decoder stage: ${cause.getMessage}")
    super.onUpstreamFailure(cause, ctx)
  }
}
