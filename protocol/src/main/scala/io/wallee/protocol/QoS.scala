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
 * Delivery guarantees defined by MQTT 3.1.1.
 *
 * @see http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718099
 */
sealed trait QoS

/**
 * A message will be delivered at most once. It may be delivered not at all.
 */
object AtMostOnce extends QoS

/**
 * A message will be delivered at least once. It may be delivered more than once.
 */
object AtLeastOnce extends QoS

/**
 * A message will be delivered exactly once. This is obviously in many situations the most desirable but always the
 * most expensive option.
 */
object ExactlyOnce extends QoS

/**
 * Reserved for future use. MUST NOT be used.
 */
object Reserved extends QoS
