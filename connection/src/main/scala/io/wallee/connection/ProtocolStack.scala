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

package io.wallee.connection

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{ BidiFlow, Tcp }
import akka.util.ByteString
import io.wallee.codec.{ MqttCodec, MqttFraming }
import io.wallee.connection.monitor.{ MqttPacketsLogging, NetworkPacketsLogging }
import io.wallee.protocol.MqttPacket

object ProtocolStack {

  def apply(conn: Tcp.IncomingConnection, level: Logging.LogLevel)(implicit system: ActorSystem): BidiFlow[MqttPacket, ByteString, ByteString, MqttPacket, NotUsed] =
    MqttPacketsLogging(conn, level)(system)
      .atop(MqttCodec(conn)(system)
        .atop(MqttFraming(conn)(system)
          .atop(NetworkPacketsLogging(conn, level)(system))))
}
