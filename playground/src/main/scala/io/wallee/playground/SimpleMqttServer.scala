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

package io.wallee.playground

import akka.actor.ActorSystem
import akka.stream.scaladsl.Tcp
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.typesafe.config.ConfigFactory
import io.wallee.connection.impl.DefaultMqttConnectionFactory
import io.wallee.shared.plugin.auth.AuthenticationPluginFactoryLoader

object SimpleMqttServer {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("SimpleMqttServer")
    val (address, port) = ("127.0.0.1", 1883)
    server(system, address, port)
  }

  def server(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    implicit val materializer = ActorMaterializer(ActorMaterializerSettings(sys))

    val config = ConfigFactory.load()

    val authenticationPluginFactoryLoader = new
        AuthenticationPluginFactoryLoader(Thread.currentThread().getContextClassLoader)
    val authenticationPluginFactory = authenticationPluginFactoryLoader.load()
    val authenticationPlugin = authenticationPluginFactory.apply(config)

    val connectionFactory = new DefaultMqttConnectionFactory(config, authenticationPlugin)

    val connections = Tcp().bind(address, port)

    connections runForeach { conn =>
      system.log.info(s"Client connected: [${conn.remoteAddress}]")

      conn.handleWith(connectionFactory(conn))
    }
  }
}
