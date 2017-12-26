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
import io.wallee.connection.MqttPacketProcessor
import io.wallee.protocol._
import io.wallee.shared.logging.TcpConnectionLogging

/** An [[MqttPacketProcessor]] for handling [[PingReq]] packets, i.e. responding with a [[PingResp]].
 */
final class PingReqProcessor(protected[this] val connection: Tcp.IncomingConnection)(protected[this] implicit val system: ActorSystem)
  extends MqttPacketProcessor[PingReq, PingResp] with TcpConnectionLogging {

  override def process(pingReq: PingReq): Option[PingResp] = Some(PingResp())
}
