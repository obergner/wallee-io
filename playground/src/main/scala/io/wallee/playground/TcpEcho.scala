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
import akka.stream.scaladsl.{Flow, Sink, Source, Tcp}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString

import scala.util.{Failure, Success}

object TcpEcho {

  /** Use without parameters to start both client and
    * server.
    *
    * Use parameters `server 0.0.0.0 6001` to start server listening on port 6001.
    *
    * Use parameters `client 127.0.0.1 6001` to start client connecting to
    * server on 127.0.0.1:6001.
    *
    */
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      val system = ActorSystem("ClientAndServer")
      val (address, port) = ("127.0.0.1", 6000)
      server(system, address, port)
      client(system, address, port)
    } else {
      val (address, port) =
        if (args.length == 3) {
          (args(1), args(2).toInt)
        }
        else {
          ("127.0.0.1", 6000)
        }
      if (args(0) == "server") {
        val system = ActorSystem("Server")
        server(system, address, port)
      } else if (args(0) == "client") {
        val system = ActorSystem("Client")
        client(system, address, port)
      }
    }
  }

  def server(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorMaterializer(ActorMaterializerSettings(sys))

    val handler = Sink.foreach[Tcp.IncomingConnection] { conn =>
      system.log.info(s"Client connected from: ${conn.remoteAddress}")
      conn handleWith Flow[ByteString]
    }

    val connections = Tcp().bind(address, port)
    val binding = connections.to(handler).run()

    binding.onComplete {
      case Success(b) =>
        system.log.info(s"Server started listening on: ${b.localAddress}")
      case Failure(e) =>
        system.log.error(e, s"Server could not bind to $address:$port: ${e.getMessage}")
        system.terminate()
    }

  }

  def client(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorMaterializer(ActorMaterializerSettings(sys))

    val testInput = ('a' to 'z').map(ByteString(_))

    val result = Source(testInput).via(Tcp().outgoingConnection(address, port)).
      runFold(ByteString.empty) { (acc, in) ⇒ acc ++ in }

    result.onComplete {
      case Success(res) =>
        system.log.info(s"Result: " + res.utf8String)
        system.log.info("Shutting down client")
        system.terminate()
      case Failure(e) =>
        system.log.error(e, s"Failed to send message: ${e.getMessage}")
        system.terminate()
    }
  }
}
