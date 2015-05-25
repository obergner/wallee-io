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

import akka.util.ByteString

/** Commonly used utilities.
 */
package object shared {

  /** Convert `buffer` into a HEX string.
   *
   *  @param buffer [[ByteString]] to convert
   *  @return HEX representation of `buffer`
   */
  def byteStringToHex(buffer: ByteString): String =
    buffer.foldLeft[StringBuilder](new StringBuilder)({ (hexBuf, b) => hexBuf.append("%02x".format(b)) }).toString()
}
