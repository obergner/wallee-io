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

package io.wallee.playground

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{ Sink, Tcp }
import com.typesafe.config.ConfigFactory
import io.wallee.connection.impl.DefaultMqttConnectionFactory

import scala.util.{ Failure, Success }

object SimpleMqttServer {

  /** Use without parameters to start both client and
   *  server.
   *
   *  Use parameters `server 0.0.0.0 6001` to start server listening on port 6001.
   *
   *  Use parameters `client 127.0.0.1 6001` to start client connecting to
   *  server on 127.0.0.1:6001.
   *
   */
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("SimpleMqttServer")
    val (address, port) = ("127.0.0.1", 1883)
    server(system, address, port)
  }

  def server(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorFlowMaterializer()
    val log = Logging(system, getClass)

    val config = ConfigFactory.load()
    val connectionFactory = new DefaultMqttConnectionFactory(config, system)

    val handler = Sink.foreach[Tcp.IncomingConnection] { conn =>
      log.info(s"Client connected: [${conn.remoteAddress}]")

      conn.handleWith(connectionFactory(conn))
    }

    val connections = Tcp().bind(address, port)
    val binding = connections.to(handler).run()

    binding.onComplete {
      case Success(b) =>
        log.info(s"Server started: [listen: ${b.localAddress}]")
      case Failure(e) =>
        log.error(e, s"Server start failed, could not bind to $address:$port: ${e.getMessage}")
        system.shutdown()
    }
  }
}
