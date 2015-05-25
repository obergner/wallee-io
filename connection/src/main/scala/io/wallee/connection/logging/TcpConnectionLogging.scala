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

package io.wallee.connection.logging

import akka.actor.ActorSystem
import akka.event.{ LogSource, Logging }
import akka.stream.scaladsl.Tcp

/** Support meaningful log output by including remote address.
 */
trait TcpConnectionLogging {

  protected[this] val system: ActorSystem

  protected[this] def connection: Tcp.IncomingConnection

  protected[this] implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    override def genString(t: AnyRef): String = s"MQTTClientConnection[${connection.remoteAddress}]"
  }

  protected[this] val log = Logging(system, this)
}
