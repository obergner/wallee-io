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

import akka.util.ByteString
import io.wallee.protocol.Subscription.TopicFilter
import io.wallee.protocol.{ MalformedMqttPacketException, PacketIdentifier, Subscribe, Subscription }

import scala.util.{ Failure, Success, Try }

/** An [[MqttPacketDecoder]] for [[Subscribe]] packets.
 *
 *  @see [[http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718037]]
 */
@SuppressWarnings(Array("org.brianmckenna.wartremover.warts.MutableDataStructures"))
object SubscribeDecoder extends MqttPacketDecoder[Subscribe](PacketType.Subscribe) {

  import MqttPacketDecoder._

  override protected[this] def doDecode(frame: MqttFrame): Try[Subscribe] = {
    if (frame.fixedHeaderFlags != 0x02) {
      Failure(MalformedMqttPacketException("[MQTT-3.8.1-1] Illegal fixed header flags in SUBSCRIBE packet"))
    } else {
      decodeUint16(frame.variableHeaderPlusPayload).flatMap[Subscribe]({
        case (packetId, remainder) =>
          decodeSubscriptions(remainder).flatMap[Subscribe]({ subscriptions =>
            if (subscriptions.nonEmpty) {
              Success(Subscribe(PacketIdentifier(packetId), subscriptions))
            } else {
              Failure(MalformedMqttPacketException("SUBSCRIBE packet contains empty topic filters list"))
            }
          })
      })
    }
  }

  private def decodeSubscriptions(payload: ByteString): Try[List[Subscription]] = {
    decodeSubscriptionsAcc(payload, scala.collection.mutable.ArrayBuffer[Subscription]())
  }

  private def decodeSubscriptionsAcc(payload: ByteString, acc: scala.collection.mutable.ArrayBuffer[Subscription]): Try[List[Subscription]] = {
    if (payload.isEmpty) {
      Success(acc.toList)
    } else {
      decodeUtf8String(payload).flatMap[List[Subscription]]({
        case (topicFilter, remainder) =>
          if (remainder.isEmpty) {
            Failure(MalformedMqttPacketException("Topic filter misses QoS byte"))
          } else {
            val requestedQoS = decodeQoS(remainder.head)
            acc.append(Subscription(TopicFilter(topicFilter), requestedQoS))
            decodeSubscriptionsAcc(remainder.tail, acc)
          }
      })
    }
  }
}
