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

package io.wallee.connection.auth

import java.net.InetSocketAddress
import java.util.Optional

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Flow, Tcp }
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import io.wallee.protocol.{ ClientId, Connect, ConnectReturnCode, ProtocolLevel, QoS, Topic }
import io.wallee.shared.plugin.auth.noop.NoopAuthenticationPlugin
import io.wallee.spi.auth.Credentials
import org.scalatest.{ FlatSpec, Matchers }

class ConnectProcessorSpec extends FlatSpec with Matchers {

  final implicit val as: ActorSystem = ActorSystem()

  final implicit val fm: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(as))

  val connection = Tcp.IncomingConnection(new InetSocketAddress(111), new InetSocketAddress(222), Flow[ByteString])

  "All credentials accepting ConnectProcessor when given a Connect" should "respond with a positive Connack" in {
    val connect = Connect("MQTT", ProtocolLevel.Level4, ClientId("surgemq"), cleanSession = true, 10, "surgemq", "verysecret", QoS.AtLeastOnce, retainWill = false, Topic("will"), "send me home")

    val objectUnderTest = new ConnectProcessor(connection, new NoopAuthenticationPlugin)

    objectUnderTest.process(connect) match {
      case Some(connack) => assert(connack.returnCode == ConnectReturnCode.ConnectionAccepted)
      case _             => fail("should have returned a positive Connack")
    }
  }

  "All credentials rejecting ConnectProcessor when given a Connect" should "respond with a negative Connack" in {
    val connect = Connect("MQTT", ProtocolLevel.Level4, ClientId("surgemq"), cleanSession = true, 10, "surgemq", "verysecret", QoS.AtLeastOnce, retainWill = false, Topic("will"), "send me home")

    val objectUnderTest = new ConnectProcessor(connection, (clientCredentials: Credentials) => Optional.empty())

    objectUnderTest.process(connect) match {
      case Some(connack) => assert(connack.returnCode == ConnectReturnCode.BadUsernameOrPassword)
      case _             => fail("should have returned a positive Connack")
    }
  }
}
