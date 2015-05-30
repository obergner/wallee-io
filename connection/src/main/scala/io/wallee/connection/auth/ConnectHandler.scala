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

package io.wallee.connection.auth

import akka.actor.ActorSystem
import akka.stream.scaladsl.Tcp
import akka.stream.stage.{ Context, PushStage, SyncDirective }
import io.wallee.protocol.{ Connack, Connect, ConnectReturnCode }
import io.wallee.shared.logging.TcpConnectionLogging
import io.wallee.spi.auth.{ AuthenticationPlugin, Credentials }

/** A [[PushStage]] for handling [[Connect]] packets, i.e. authenticating clients.
 */
class ConnectHandler(
  protected[this] val connection: Tcp.IncomingConnection,
  authenticationPlugin:           AuthenticationPlugin
)(protected[this] implicit val system: ActorSystem)
    extends PushStage[Connect, Connack] with TcpConnectionLogging {

  override def onPush(elem: Connect, ctx: Context[Connack]): SyncDirective = {
    val clientCreds = new Credentials(elem.username.orNull, elem.password.orNull, connection.remoteAddress)
    log.debug(s"Authenticating newly connected client using $clientCreds ...")
    val principal = authenticationPlugin.authenticate(clientCreds)
    if (principal.isPresent) {
      log.info(s"Successfully authenticated newly connected client ${principal.get()}")
      ctx.push(Connack(sessionPresent = false, ConnectReturnCode.ConnectionAccepted))
    } else {
      log.warning(s"Failed to authenticate newly connected client [remote-address: ${connection.remoteAddress}")
      ctx.push(Connack(sessionPresent = false, ConnectReturnCode.BadUsernameOrPassword))
    }
  }
}
