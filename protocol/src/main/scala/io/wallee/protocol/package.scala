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

package io.wallee

import java.nio.charset.Charset

import scala.language.implicitConversions

package object protocol {

  /** Require that an [[MqttPacket]] be well-formed, i.e. that it conforms to the rules laid out in MQTT 3.1.1. Throw
   *  a [[MalformedMqttPacketException]] otherwise.
   *
   *  @param requirement The requirement to fulfill
   *  @param message Explanatory message in case `requirement` fails
   */
  @throws[MalformedMqttPacketException]
  @inline final def requireWellformed(requirement: Boolean, message: => Any) {
    if (!requirement) {
      throw new MalformedMqttPacketException("Malformed MQTT packet: " + message)
    }
  }

  /** Implicit conversion from String to [[ClientId]].
   *
   *  @param clientId String value to convert
   *  @return Conversion result
   */
  implicit def string2ClientId(clientId: String): ClientId = ClientId(clientId)

  /** Implicit conversion from [[ClientId]] to String.
   *
   *  @param clientId [[ClientId]] to convert
   *  @return Conversion result
   */
  implicit def clientId2String(clientId: ClientId): String = clientId.value

  /** Implicit conversion from String to [[Topic]].
   *
   *  @param topic String value to convert
   *  @return Conversion result
   */
  implicit def string2Topic(topic: String): Topic = Topic(topic)

  /** Implicit conversion from [[Topic]] to String.
   *
   *  @param topic [[Topic]] to convert
   *  @return Conversion result
   */
  implicit def topic2String(topic: Topic): String = topic.value

  /** How many bytes on the wire it will take to encode string.
   *
   *  @param string String to encode
   *  @return Length in bytes on the wire
   */
  def encodedLengthInBytesOf(string: String): Int = 2 + string.getBytes(Charset.forName("UTF-8")).length
}
