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

package io.wallee.connection.publish

import akka.actor.ActorSystem
import akka.stream.scaladsl.Tcp
import io.wallee.connection.MqttPacketProcessor
import io.wallee.protocol.{ MqttPacket, Puback, Publish }
import io.wallee.shared.logging.TcpConnectionLogging

/** Handle [[Publish]] messages.
 *
 *  @todo Implementation needed
 */
final class PublishProcessor(protected[this] val connection: Tcp.IncomingConnection)(protected[this] implicit val system: ActorSystem)
  extends MqttPacketProcessor[Publish, MqttPacket] with TcpConnectionLogging {

  override def process(publish: Publish): Option[MqttPacket] = {
    log.debug(s"PUBLISH:   $publish ...")
    log.info(s"PUBLISHED: $publish - confirmation pending")
    Some(Puback(publish.packetId))
  }
}
