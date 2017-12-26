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

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Flow, Tcp }
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import io.wallee.protocol.PingReq
import org.scalatest.{ FlatSpec, Matchers }

class PingReqProcessorSpec extends FlatSpec with Matchers {

  final implicit val as: ActorSystem = ActorSystem()

  final implicit val fm: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(as))

  val connection = Tcp.IncomingConnection(new InetSocketAddress(111), new InetSocketAddress(222), Flow[ByteString])

  "process when given a PingReq" should "respond with a PingResp" in {
    val pingReq = PingReq()

    val objectUnderTest = new PingReqProcessor(connection)

    objectUnderTest.process(pingReq) match {
      case Some(pingResp) => succeed
      case _              => fail("should have returned a PingResp")
    }
  }
}
