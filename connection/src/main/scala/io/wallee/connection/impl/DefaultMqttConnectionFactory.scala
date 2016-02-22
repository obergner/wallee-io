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
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import akka.util.ByteString
import com.typesafe.config.Config
import io.wallee.codec.{ DecoderStage, EncoderStage, FrameDecoderStage }
import io.wallee.connection.MqttConnectionFactory
import io.wallee.connection.auth.ConnectHandler
import io.wallee.connection.monitor.{ LogMqttPackets, LogNetworkPackets }
import io.wallee.connection.ping.PingReqHandler
import io.wallee.connection.publish.PublishHandler
import io.wallee.protocol._
import io.wallee.spi.auth.AuthenticationPlugin

/**
 */
class DefaultMqttConnectionFactory(
  config:               Config,
  authenticationPlugin: AuthenticationPlugin
)(protected[this] implicit val system: ActorSystem)
    extends MqttConnectionFactory {

  override def apply(conn: Tcp.IncomingConnection): Flow[ByteString, ByteString, NotUsed] =
    Flow.fromGraph[ByteString, ByteString, NotUsed](GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      system.log.info(s"[${conn.remoteAddress}] Creating connection handler pipeline ...")

      val parallelPipeline: Flow[MqttPacket, MqttPacket, NotUsed] = parallelProcessingPipelines(conn)

      val input = builder.add(Flow[ByteString])
      val output = builder.add(Flow[ByteString])

      input.out
        .via(new LogNetworkPackets(conn, "RCVD", Logging.DebugLevel))
        .via(new FrameDecoderStage(conn))
        .via(new DecoderStage(conn))
        .via(new LogMqttPackets(conn, "RCVD", Logging.DebugLevel))
        .via(parallelProcessingPipelines(conn))
        .via(new LogMqttPackets(conn, "SEND", Logging.DebugLevel))
        .via(new EncoderStage(conn))
        .via(new LogNetworkPackets(conn, "SEND", Logging.DebugLevel))
        .~>(output.in)

      system.log.info(s"[${conn.remoteAddress}] Connection handler pipeline created")

      FlowShape(input.in, output.out)
    })

  private[this] def parallelProcessingPipelines(conn: Tcp.IncomingConnection): Flow[MqttPacket, MqttPacket, NotUsed] =
    Flow.fromGraph[MqttPacket, MqttPacket, NotUsed](GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val packetRouter: PacketRouting = new PacketRouting
      val fanOut = builder.add(packetRouter)
      val fanIn = builder.add(Merge[MqttPacket](4))

      fanOut.out0
        .via(new ConnectHandler(conn, authenticationPlugin))
        .~>(fanIn.in(0))

      fanOut.out1
        .via(new PingReqHandler(conn))
        .~>(fanIn.in(1))

      fanOut.out2
        .via(new PublishHandler(conn))
        .~>(fanIn.in(2))

      fanOut.out3
        .~>(fanIn.in(3))

      FlowShape(fanOut.in, fanIn.out)
    })
}

final class PacketRouting extends GraphStage[FanOutShape4[MqttPacket, Connect, PingReq, Publish, MqttPacket]] {

  val incomingMqttPackets = Inlet[MqttPacket]("PacketRouting.incomingMqttPackets")

  val forwardedConnectPackets = Outlet[Connect]("PacketRouting.forwardedConnectPackets")

  val forwardedPingRequests = Outlet[PingReq]("PacketRouting.forwardedPingRequests")

  val forwardedPublishPackets = Outlet[Publish]("PacketRouting.forwardedPublishPackets")

  val forwardedMqttPackets = Outlet[MqttPacket]("PacketRouting.forwardedMqttPackets")

  override def shape: FanOutShape4[MqttPacket, Connect, PingReq, Publish, MqttPacket] = {
    new FanOutShape4[MqttPacket, Connect, PingReq, Publish, MqttPacket]("PacketRoutingShape")
  }

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      setHandler(incomingMqttPackets, new InHandler {
        @throws[Exception](classOf[Exception])
        override def onPush(): Unit = {
          grab(incomingMqttPackets) match {
            case p: Connect    => push(forwardedConnectPackets, p)
            case p: PingReq    => push(forwardedPingRequests, p)
            case p: Publish    => push(forwardedPublishPackets, p)
            case p: MqttPacket => push(forwardedMqttPackets, p)
          }
        }
      })

      setHandler(forwardedConnectPackets, new OutHandler {
        @throws[Exception](classOf[Exception])
        override def onPull(): Unit = {
          pull(incomingMqttPackets)
        }
      })

      setHandler(forwardedPingRequests, new OutHandler {
        @throws[Exception](classOf[Exception])
        override def onPull(): Unit = {
          pull(incomingMqttPackets)
        }
      })

      setHandler(forwardedPublishPackets, new OutHandler {
        @throws[Exception](classOf[Exception])
        override def onPull(): Unit = {
          pull(incomingMqttPackets)
        }
      })

      setHandler(forwardedMqttPackets, new OutHandler {
        @throws[Exception](classOf[Exception])
        override def onPull(): Unit = {
          pull(incomingMqttPackets)
        }
      })
    }
}
