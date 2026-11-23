/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.util

import java.util.Locale
import java.util.logging.Logger

/**
 * Can be used to measure the time needed to execute a provided expression.
 */
object Timer {
  def apply[T](description: String)(f: => T)(implicit logger: Logger = Logger.getAnonymousLogger): T = {
    val startTime = System.currentTimeMillis

    val result = f

    val elapsedTime = (System.currentTimeMillis - startTime)
    logger.info(description + " took " + formatDuration(elapsedTime))

    result
  }

  /** Formats a duration in milliseconds for messages, e.g. "312ms", "4.567s", "29min 20s" or "2h 0min". */
  def formatDuration(millis: Long): String = {
    val seconds = millis / 1000
    if (millis < 1000) {
      s"${millis}ms"
    } else if (seconds < 60) {
      "%.3fs".formatLocal(Locale.ROOT, millis.toDouble / 1000)
    } else if (seconds < 3600) {
      s"${seconds / 60}min ${seconds % 60}s"
    } else {
      s"${seconds / 3600}h ${seconds % 3600 / 60}min"
    }
  }
}