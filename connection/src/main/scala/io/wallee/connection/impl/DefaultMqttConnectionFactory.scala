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

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.FlexiRoute.{ DemandFromAll, RouteLogic }
import akka.stream.scaladsl._
import akka.stream.stage.{ Context, PushStage, SyncDirective }
import akka.stream.{ FanOutShape4, FanOutShape3, OperationAttributes }
import akka.util.ByteString
import com.typesafe.config.Config
import io.wallee.codec.{ DecoderStage, EncoderStage, FrameDecoderStage, MqttFrame }
import io.wallee.connection.MqttConnectionFactory
import io.wallee.connection.auth.ConnectHandler
import io.wallee.connection.error.DecodingErrorLogger
import io.wallee.connection.monitor.{ LogMqttPackets, LogNetworkPackets }
import io.wallee.connection.ping.PingReqHandler
import io.wallee.connection.publish.PublishHandler
import io.wallee.protocol.{ Publish, Connect, MqttPacket, PingReq }
import io.wallee.shared.logging.TcpConnectionLogging
import io.wallee.spi.auth.AuthenticationPlugin

/**
 */
class DefaultMqttConnectionFactory(
  config:                              Config,
  authenticationPlugin:                AuthenticationPlugin,
  protected[this] implicit val system: ActorSystem
)
    extends MqttConnectionFactory {

  override def apply(conn: Tcp.IncomingConnection): Flow[ByteString, ByteString, _] = Flow() { implicit builder: FlowGraph.Builder[Unit] =>
    import FlowGraph.Implicits._

    val parellelPipeline: Flow[MqttPacket, MqttPacket, _] = parallelProcessingPipelines(conn)

    val input = builder.add(Flow[ByteString])
    val output = builder.add(Flow[ByteString])

    input.outlet
      .transform[ByteString](() => new LogNetworkPackets(conn, "RCVD", Logging.DebugLevel))
      .transform[MqttFrame](() => new FrameDecoderStage(conn))
      .transform[MqttPacket](() => new DecoderStage(conn))
      .transform[MqttPacket](() => new DecodingErrorLogger(conn))
      .transform[MqttPacket](() => new LogMqttPackets(conn, "RCVD", Logging.DebugLevel))
      .~>(parellelPipeline)
      .transform[MqttPacket](() => new LogMqttPackets(conn, "SEND", Logging.DebugLevel))
      .transform[ByteString](() => new EncoderStage(conn))
      .transform[ByteString](() => new LogNetworkPackets(conn, "SEND", Logging.DebugLevel))
      .~>(output.inlet)

    (input.inlet, output.outlet)
  }

  private[this] def parallelProcessingPipelines(conn: Tcp.IncomingConnection): Flow[MqttPacket, MqttPacket, _] =
    Flow() { implicit builder: FlowGraph.Builder[Unit] =>
      import FlowGraph.Implicits._

      val packetRouter: PacketRouting = new PacketRouting
      val fanOut = builder.add(packetRouter)
      val fanIn = builder.add(Merge[MqttPacket](4))

      fanOut.out0
        .transform(() => new ConnectHandler(conn, authenticationPlugin))
        .~>(fanIn.in(0))

      fanOut.out1
        .transform(() => new PingReqHandler(conn))
        .~>(fanIn.in(1))

      fanOut.out2
        .transform(() => new PublishHandler(conn))
        .~>(fanIn.in(2))

      fanOut.out3
        .~>(fanIn.in(3))

      (fanOut.in, fanIn.out)
    }
}

final class PacketRouting
    extends FlexiRoute[MqttPacket, FanOutShape4[MqttPacket, Connect, PingReq, Publish, MqttPacket]](
      new FanOutShape4[MqttPacket, Connect, PingReq, Publish, MqttPacket]("packetRouter"), OperationAttributes.name("packetRouter")
    ) {

  override def createRouteLogic(s: FanOutShape4[MqttPacket, Connect, PingReq, Publish, MqttPacket]): RouteLogic[MqttPacket] = new RouteLogic[MqttPacket] {
    override def initialState: State[_] = State[Any](DemandFromAll(s)) {
      (ctx, _, packet) =>
        packet match {
          case p: Connect    => ctx.emit[Connect](s.out0)(p)
          case p: PingReq    => ctx.emit[PingReq](s.out1)(p)
          case p: Publish    => ctx.emit[Publish](s.out2)(p)
          case p: MqttPacket => ctx.emit[MqttPacket](s.out3)(p)
        }

        SameState
    }
  }
}

final class ConnectionClose(protected[this] val connection: Tcp.IncomingConnection)(protected[this] implicit val system: ActorSystem)
    extends PushStage[MqttPacket, MqttPacket] with TcpConnectionLogging {

  override def onPush(elem: MqttPacket, ctx: Context[MqttPacket]): SyncDirective = {
    log.warning(s"Closing connetion to remote client [${connection.remoteAddress} ...")
    log.warning(s"Connetion to remote client [${connection.remoteAddress} closed")
    ctx.push(elem)
  }
}