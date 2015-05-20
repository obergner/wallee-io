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

package io.wallee.protocol

/**
 * MQTT CONNECT packet.
 *
 * @see http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718028
 */
final case class Connect(
                          protocolName: String,
                          protocolLevel: ProtocolLevel,
                          clientId: ClientId,
                          cleanSession: Boolean,
                          keepAliveSecs: Int,
                          username: Option[String],
                          password: Option[String],
                          willQoS: QoS,
                          retainWill: Boolean,
                          willTopic: Option[Topic],
                          willMessage: Option[String]
                          )
  extends MqttPacket {
  require(willQoS != Reserved, "QoS 'Reserved' MUST NOT be used")
  require(keepAliveSecs <= Connect.MaxKeepAliveSecs, "Keep alive must not exceed 2^16 seconds")
  require(
    (willTopic.isDefined && willMessage.isDefined) || (willTopic.isEmpty && willMessage.isEmpty),
    "Either will topic and will message need to be given, or neither"
  )

  override protected def remainingLength: Int = {
    Connect.VariableHeaderBaseLengthInBytes +
      encodedLengthInBytesOf(protocolName) +
      encodedLengthInBytesOf(clientId) +
      username.map(encodedLengthInBytesOf).getOrElse(0) +
      password.map(encodedLengthInBytesOf).getOrElse(0) +
      willTopic.map(_.value).map(encodedLengthInBytesOf).getOrElse(0) +
      willMessage.map(encodedLengthInBytesOf).getOrElse(0)
  }
}

object Connect {

  val VariableHeaderBaseLengthInBytes: Int = 4

  val MaxKeepAliveSecs: Int = scala.math.pow(2, 16).toInt

  def apply(
             protocolName: String,
             protocolLevel: ProtocolLevel,
             clientId: ClientId,
             cleanSession: Boolean,
             keepAliveSecs: Int,
             username: String,
             password: String,
             willQoS: QoS,
             retainWill: Boolean,
             willTopic: Topic,
             willMessage: String
             ): Connect = apply(
    protocolName,
    protocolLevel,
    clientId,
    cleanSession,
    keepAliveSecs,
    Some(username),
    Some(password),
    willQoS,
    retainWill,
    Some(willTopic),
    Some(willMessage)
  )

  def apply(
             protocolName: String,
             protocolLevel: ProtocolLevel,
             clientId: ClientId,
             cleanSession: Boolean,
             keepAliveSecs: Int,
             username: String,
             password: String
             ): Connect = apply(
    protocolName,
    protocolLevel,
    clientId,
    cleanSession,
    keepAliveSecs,
    Some(username),
    Some(password),
    AtMostOnce,
    retainWill = false,
    None,
    None
  )

  def apply(
             protocolName: String,
             protocolLevel: ProtocolLevel,
             clientId: ClientId,
             cleanSession: Boolean,
             keepAliveSecs: Int
             ): Connect = apply(
    protocolName,
    protocolLevel,
    clientId,
    cleanSession,
    keepAliveSecs,
    None,
    None,
    AtMostOnce,
    retainWill = false,
    None,
    None
  )
}

