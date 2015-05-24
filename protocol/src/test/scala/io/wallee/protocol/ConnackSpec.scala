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

import org.scalatest.{ FlatSpec, Matchers }

class ConnackSpec extends FlatSpec with Matchers {

  "A Connack packet" should "report correct lengthInBytes" in {
    val expectedLength = 2 + // Fixed header
      +2 // Variable header

    val underTest = Connack(sessionPresent = false, ConnectReturnCode.ConnectionAccepted)

    assert(underTest.lengthInBytes == expectedLength)
  }
}
