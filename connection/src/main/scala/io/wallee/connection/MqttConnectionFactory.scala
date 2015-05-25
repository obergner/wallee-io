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

package io.wallee.connection

import akka.stream.Graph
import akka.stream.scaladsl.{ Flow, Tcp }
import akka.util.ByteString

/** Create an MQTT connection represented by a [[Graph]] of interconnected processing stages: [[io.wallee.codec.DecoderStage]],
 *  [[io.wallee.codec.EncoderStage]], ...
 */
trait MqttConnectionFactory extends (Tcp.IncomingConnection => Flow[ByteString, ByteString, _])
