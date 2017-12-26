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

import java.net.InetSocketAddress
import java.nio.charset.Charset

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Flow, Tcp }
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import io.wallee.protocol.{ PacketIdentifier, Publish, QoS, Topic }
import org.scalatest.{ FlatSpec, Matchers }

class PublishProcessorSpec extends FlatSpec with Matchers {

  behavior of "PublishProcessor"

  final implicit val as: ActorSystem = ActorSystem()

  final implicit val fm: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(as))

  val connection: Tcp.IncomingConnection = Tcp.IncomingConnection(new InetSocketAddress(111), new InetSocketAddress(222), Flow[ByteString])

  "process" should "respond with proper Puback" in {
    val publish = Publish(dup = false, QoS.AtLeastOnce, retain = false, Topic("/test"), PacketIdentifier(1), ByteString("test", Charset.forName("UTF-8")))

    val objectUnderTest = new PublishProcessor(connection)

    objectUnderTest.process(publish) match {
      case Some(puback) => assert(puback.packetId == publish.packetId)
      case None         => fail("Should have responded with a Puback packet")
    }
  }
}
