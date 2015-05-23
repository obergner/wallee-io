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

/** CONNACK packet.
 *
 *  @see http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718033
 */
final case class Connack(sessionPresent: Boolean, returnCode: ConnectReturnCode) extends MqttPacket {

  /** This MQTT packet's remaining length, i.e. its length on the wire sans its fixed header.
   *
   *  @return Length on the wire sans fixed header
   */
  override protected def remainingLength: Int = 2
}

/** Trait defining possible return codes in response to a Connect packet.
 */
sealed trait ConnectReturnCode

object ConnectionAccepted extends ConnectReturnCode

object UnacceptableProtocolVersion extends ConnectReturnCode

object IdentifierRejected extends ConnectReturnCode

object ServerUnavailable extends ConnectReturnCode

object BadUsernameOrPassword extends ConnectReturnCode

object NotAuthorized extends ConnectReturnCode
