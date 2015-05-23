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

/** Signals an [[MqttPacket]] - either in serialized or deserialized form - that does not conform to the rules laid out
  * by MQTT 3.1.1.
  *
  * @param msg Explanatory message
  */
case class MalformedMqttPacketException(msg: String) extends Exception(msg)
