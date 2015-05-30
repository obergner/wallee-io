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

package io.wallee.connection.ping

import akka.actor.ActorSystem
import akka.stream.scaladsl.Tcp
import akka.stream.stage.{ Context, PushStage, SyncDirective }
import io.wallee.protocol._
import io.wallee.shared.logging.TcpConnectionLogging

/** A [[PushStage]] for handling [[PingReq]] packets, i.e. responding with a [[PingResp]].
 */
class PingReqHandler(protected[this] val connection: Tcp.IncomingConnection)(protected[this] implicit val system: ActorSystem)
    extends PushStage[PingReq, PingResp] with TcpConnectionLogging {

  override def onPush(elem: PingReq, ctx: Context[PingResp]): SyncDirective = {
    log.debug(s"Received PingReq - sending PingResp")
    ctx.push(PingResp())
  }
}
