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
