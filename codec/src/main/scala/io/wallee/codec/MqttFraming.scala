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

package io.wallee.codec

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.BidiShape
import akka.stream.javadsl.BidiFlow
import akka.stream.scaladsl.{ Flow, GraphDSL, Tcp }
import akka.util.ByteString

object MqttFraming {

  def apply(conn: Tcp.IncomingConnection)(implicit system: ActorSystem): BidiFlow[ByteString, ByteString, ByteString, MqttFrame, NotUsed] =
    BidiFlow.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      val outbound = builder.add(Flow[ByteString])
      val inbound = builder.add(Flow[ByteString].via(FrameDecoderStage(conn)(system)))

      BidiShape.fromFlows(outbound, inbound)
    })
}
