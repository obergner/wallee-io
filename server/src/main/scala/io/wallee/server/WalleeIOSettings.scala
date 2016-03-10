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

import akka.actor.{ ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

class WalleeIOSettingsImpl(config: Config) extends Extension with WalleeIOConfig {

  val bindAddress: String = config.getString("walleeio.mqtt-server.bind-address")

  val bindPort: Int = config.getInt("walleeio.mqtt-server.bind-port")

  val bindTimeout: Duration = Duration.fromNanos(config.getDuration("walleeio.mqtt-server.bind-timeout").getNano)

  // TODO: ExecutionContext used by application should be configurable
  val defaultExecutionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
}

object WalleeIOSettings extends ExtensionId[WalleeIOSettingsImpl] with ExtensionIdProvider {

  override def createExtension(system: ExtendedActorSystem): WalleeIOSettingsImpl = new WalleeIOSettingsImpl(system.settings.config)

  override def lookup(): ExtensionId[_ <: Extension] = WalleeIOSettings
}
