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

package io.wallee.connection.impl

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.config.Config
import io.wallee.connection.auth.ConnectProcessor
import io.wallee.connection.ping.PingReqProcessor
import io.wallee.connection.publish.PublishProcessor
import io.wallee.connection.{ MqttConnectionFactory, ProtocolHandler, ProtocolStack }
import io.wallee.protocol.MqttPacket
import io.wallee.spi.auth.AuthenticationPlugin

class DefaultMqttConnectionFactory(
    config:               Config,
    authenticationPlugin: AuthenticationPlugin)(protected[this] implicit val system: ActorSystem)
  extends MqttConnectionFactory {

  override def apply(conn: Tcp.IncomingConnection): Flow[ByteString, ByteString, NotUsed] = {
    ProtocolStack(conn, Logging.DebugLevel).reversed.join(Flow[MqttPacket].via(protocolHandler(conn)))
  }

  private[this] def protocolHandler(conn: Tcp.IncomingConnection): ProtocolHandler = {
    new ProtocolHandler(new ConnectProcessor(conn, authenticationPlugin), new PingReqProcessor(conn), new PublishProcessor(conn), conn)
  }
}
