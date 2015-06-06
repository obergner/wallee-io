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

/** Represents a client's interest in one or more [[Topic]]s. Combines
 *
 *  - a [[Subscription#TopicFilter topic filter]] and
 *  - [[QoS quality of service]] requested for matching [[Topic]]s
 *
 *  @see [[http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718066]]
 */
final case class Subscription(topicFilter: Subscription.TopicFilter, requestedQoS: QoS)

object Subscription {

  final case class TopicFilter(filter: String)

  def apply(topicFiler: String, requestedQos: QoS): Subscription = Subscription(TopicFilter(topicFiler), requestedQos)
}