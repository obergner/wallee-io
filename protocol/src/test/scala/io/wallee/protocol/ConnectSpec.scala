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

import java.nio.charset.Charset

import org.scalatest.{ FlatSpec, Matchers }

class ConnectSpec extends FlatSpec with Matchers {

  "A Connect packet with all fields set" should "report correct lengthInBytes" in {
    val protocolName = "MQTT"
    val clientId = "clientId"
    val username = "username"
    val password = "password"
    val willTopic = "this/is/a/test/topic"
    val willMessage = "Good bye, cruel world"

    val expectedLength = 2 + // Fixed header
      6 + protocolName.getBytes(Charset.forName("UTF-8")).length + // Variable header
      2 + clientId.getBytes(Charset.forName("UTF-8")).length +
      2 + username.getBytes(Charset.forName("UTF-8")).length +
      2 + password.getBytes(Charset.forName("UTF-8")).length +
      2 + willTopic.getBytes(Charset.forName("UTF-8")).length +
      2 + willMessage.getBytes(Charset.forName("UTF-8")).length

    val underTest = Connect(
      protocolName,
      Level4,
      clientId,
      cleanSession = false,
      15,
      username,
      password,
      ExactlyOnce,
      retainWill = false,
      willTopic,
      willMessage
    )

    assert(underTest.lengthInBytes == expectedLength)
  }

  "A Connect packet with only required fields set" should "report correct lengthInBytes" in {
    val protocolName = "MQTT_5.0"
    val clientId = "yet another client $id %&&&&"

    val expectedLength = 2 + // Fixed header
      6 + protocolName.getBytes(Charset.forName("UTF-8")).length + // Variable header
      2 + clientId.getBytes(Charset.forName("UTF-8")).length

    val underTest = Connect(protocolName, Level4, clientId, cleanSession = false, 15)

    assert(underTest.lengthInBytes == expectedLength)
  }

  "A Connect packet with will topic yet without will message" should "throw IllegalArgumentException upon creation" in {
    val protocolName = "MQTT"
    val clientId = "clientId"
    val username = "username"
    val password = "password"
    val willTopic: Topic = "this/is/a/test/topic"

    intercept[MalformedMqttPacketException] {
      Connect(
        protocolName,
        Level4,
        clientId,
        cleanSession = false,
        15,
        Some(username),
        Some(password),
        ExactlyOnce,
        retainWill = false,
        Some(willTopic),
        None
      )
    }
  }

  "A Connect packet with illegal keep alive time" should "throw IllegalArgumentException upon creation" in {
    val protocolName = "MQTT"
    val clientId = "clientId"
    val username = "username"
    val password = "password"
    val willTopic: Topic = "this/is/a/test/topic"
    val willMessage = "This is a will message"
    val keepAliveSecs = (scala.math.pow(2, 16) + 1).toInt

    intercept[MalformedMqttPacketException] {
      Connect(
        protocolName,
        Level4,
        clientId,
        cleanSession = false,
        keepAliveSecs,
        Some(username),
        Some(password),
        ExactlyOnce,
        retainWill = false,
        Some(willTopic),
        Some(willMessage)
      )
    }
  }
}
