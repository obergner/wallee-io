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

package io.wallee.server

import akka.actor.ActorSystem
import akka.stream.scaladsl.Tcp.ServerBinding
import akka.stream.scaladsl.{ Sink, Tcp }
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import com.typesafe.config.ConfigFactory
import io.wallee.connection.impl.DefaultMqttConnectionFactory
import io.wallee.shared.plugin.auth.AuthenticationPluginFactoryLoader

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

class MqttServer(bindAddress: String, bindPort: Int, bindTimeout: Duration, executionContext: ExecutionContext) {

  implicit val system: ActorSystem = ActorSystem("mqtt-server")

  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))

  implicit val ec: ExecutionContext = executionContext

  def start(): Try[MqttServer.Handle] = {
    try {
      val serverBinding = doStart()
      val handle = newHandle(serverBinding)
      Success(handle)
    } catch {
      case ex: Throwable => Failure(ex)
    }
  }

  private def newHandle(serverBinding: ServerBinding): MqttServer.Handle = {
    new MqttServer.Handle {
      override def stop(): Try[Unit] = {
        try {
          val unbound: Future[Unit] = serverBinding.unbind().andThen[Unit] {
            case Success(_) =>
              system.log.info(s"MQTT server successfully unbound from [$bindAddress:$bindPort]")
              val _ = system.terminate()
            case Failure(ex) =>
              system.log.error(ex, s"Failed to unbind MQTT server from [$bindAddress:$bindPort]: ${ex.getMessage}")
              val _ = system.terminate()
          }
          val _ = Await.result(unbound, bindTimeout)
          Success(())
        } catch {
          case ex: Throwable => Failure(ex)
        }
      }
    }
  }

  private[this] def doStart(): ServerBinding = {

    val config = ConfigFactory.load()

    val authenticationPluginFactoryLoader = new AuthenticationPluginFactoryLoader(Thread.currentThread().getContextClassLoader)
    val authenticationPluginFactory = authenticationPluginFactoryLoader.load()
    val authenticationPlugin = authenticationPluginFactory(config)

    val connectionFactory = new DefaultMqttConnectionFactory(config, authenticationPlugin)

    val connectionHandler = Sink.foreach[Tcp.IncomingConnection] { conn =>
      system.log.info(s"Client connected: [${conn.remoteAddress}]")

      val _ = conn.handleWith(connectionFactory(conn))
    }

    val serverBindingFut: Future[ServerBinding] = Tcp().bind(bindAddress, bindPort).to(connectionHandler).run()
    Await.result(serverBindingFut, bindTimeout)
  }
}

object MqttServer {

  trait Handle {

    def stop(): Try[Unit]
  }

  def apply(bindAddress: String, bindPort: Int, bindTimeout: Duration, executionContext: ExecutionContext): MqttServer =
    new MqttServer(bindAddress, bindPort, bindTimeout, executionContext)

  def apply(): MqttServer = {
    val config = ConfigFactory.load()
    val settings = new WalleeIOSettingsImpl(config)
    new MqttServer(settings.bindAddress, settings.bindPort, settings.bindTimeout, settings.defaultExecutionContext)
  }
}
