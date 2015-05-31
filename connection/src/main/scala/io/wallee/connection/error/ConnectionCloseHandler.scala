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
import akka.util.ByteString
import io.wallee.protocol.MalformedMqttPacketException
import io.wallee.shared.logging.TcpConnectionLogging

/** [[PushStage]] that intercepts all [[Throwable]]s propagated downstream. It will inspect any such error and decide
 *  whether to close its associated [[Tcp.IncomingConnection connection]].
 *
 *  @param connection TCP connection this handler is associated with
 *  @param system Implicit [[ActorSystem]]
 */
final class ConnectionCloseHandler(protected[this] val connection: Tcp.IncomingConnection)(protected[this] implicit val system: ActorSystem)
    extends PushStage[ByteString, ByteString] with TcpConnectionLogging {

  override def onPush(elem: ByteString, ctx: Context[ByteString]): SyncDirective = ctx.push(elem)

  override def onUpstreamFailure(cause: Throwable, ctx: Context[ByteString]): TerminationDirective = {
    cause match {
      case ex: MalformedMqttPacketException =>
        log.error(cause, s"CLOSE: connection to client ${connection.remoteAddress} failed - will close this connection")
        ctx.finish()
      case _ => ctx.fail(cause)
    }
  }
}
