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
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source, Tcp}
import akka.stream.stage.{Context, PushStage, StatefulStage, SyncDirective}
import akka.util.ByteString

import scala.annotation.tailrec
import scala.util.{Failure, Success}

object CloseConnection {

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
    val system = ActorSystem("ClientAndServer")
    val (address, port) = ("127.0.0.1", 6000)
    server(system, address, port)
    client(system, address, port)
  }

  def server(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorFlowMaterializer()

    val handler = Sink.foreach[Tcp.IncomingConnection] { conn =>
      println("Client connected from: " + conn.remoteAddress)
      conn handleWith connectionHandler(conn)
    }

    val connections = Tcp().bind(address, port)
    val binding = connections.to(handler).run()

    binding.onComplete {
      case Success(b) =>
        println("Server started, listening on: " + b.localAddress)
      case Failure(e) =>
        println(s"Server could not bind to $address:$port: ${e.getMessage}")
        system.shutdown()
    }

  }

  private def connectionHandler(conn: Tcp.IncomingConnection): Flow[ByteString, ByteString, _] = {
    Flow[ByteString]
      .log("ECHO-IN-1")
      .transform[String](() => parseLines("\n", 4000))
      .log("ECHO-IN-2")
      .transform[String](() => new PushStage[String, String]() {
      override def onPush(elem: String, ctx: Context[String]): SyncDirective = {
        if (elem.equalsIgnoreCase("CLOSE")) {
          conn.flow.concat(Source.empty).to(Sink.cancelled)
          ctx.pushAndFinish(elem)
        } else {
          ctx.push(elem)
        }
      }
    })
      .log("ECHO-OUT-1")
      .map[ByteString](str => ByteString(str))
      .log("ECHO-OUT-2")
  }

  def parseLines(separator: String, maximumLineBytes: Int) =
    new StatefulStage[ByteString, String] {
      private val separatorBytes = ByteString(separator)
      private val firstSeparatorByte = separatorBytes.head
      private var buffer = ByteString.empty
      private var nextPossibleMatch = 0

      def initial = new State {
        override def onPush(chunk: ByteString, ctx: Context[String]): SyncDirective = {
          buffer ++= chunk
          if (buffer.size > maximumLineBytes)
            ctx.fail(new IllegalStateException(s"Read ${buffer.size} bytes " +
              s"which is more than $maximumLineBytes without seeing a line terminator"))
          else emit(doParse(Vector.empty).iterator, ctx)
        }

        @tailrec
        private def doParse(parsedLinesSoFar: Vector[String]): Vector[String] = {
          val possibleMatchPos = buffer.indexOf(firstSeparatorByte, from = nextPossibleMatch)
          if (possibleMatchPos == -1) {
            // No matching character, we need to accumulate more bytes into the buffer
            nextPossibleMatch = buffer.size
            parsedLinesSoFar
          } else if (possibleMatchPos + separatorBytes.size > buffer.size) {
            // We have found a possible match (we found the first character of the terminator
            // sequence) but we don't have yet enough bytes. We remember the position to
            // retry from next time.
            nextPossibleMatch = possibleMatchPos
            parsedLinesSoFar
          } else {
            if (buffer.slice(possibleMatchPos, possibleMatchPos + separatorBytes.size)
              == separatorBytes) {
              // Found a match
              val parsedLine = buffer.slice(0, possibleMatchPos).utf8String
              buffer = buffer.drop(possibleMatchPos + separatorBytes.size)
              nextPossibleMatch -= possibleMatchPos + separatorBytes.size
              doParse(parsedLinesSoFar :+ parsedLine)
            } else {
              nextPossibleMatch += 1
              doParse(parsedLinesSoFar)
            }
          }

        }
      }
    }

  def client(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorFlowMaterializer()

    val testInput = ByteString("ECHO ME\n")

    val result = Source.repeat(testInput)
      .map[ByteString]({ input =>
      if (scala.math.random < 0.05) {
        ByteString("CLOSE\n")
      } else {
        input
      }
    })
      .via(Tcp().outgoingConnection(address, port))
      .runFold(ByteString.empty) { (acc, in) ⇒ acc ++ in }

    result.onComplete {
      case Success(result) =>
        println(s"Result: " + result.utf8String)
        println("Shutting down client")
        system.shutdown()
      case Failure(e) =>
        println("Failure: " + e.getMessage)
        system.shutdown()
    }
  }
}
