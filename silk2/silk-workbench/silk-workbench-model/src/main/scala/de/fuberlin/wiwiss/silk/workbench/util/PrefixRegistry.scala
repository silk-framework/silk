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

package de.fuberlin.wiwiss.silk.workbench.util

import io.Source
import java.util.logging.{Level, Logger}
import de.fuberlin.wiwiss.silk.config.Prefixes

/**
 * Registry of known prefixes.
 */
object PrefixRegistry {
  private val logger = Logger.getLogger(getClass.getName)

  /**
   * Map of all known prefixes.
   */
  lazy val all: Prefixes = {
    try {
      val prefixStream = getClass.getClassLoader.getResourceAsStream("de/fuberlin/wiwiss/silk/workbench/prefixes.csv")
      val prefixSource = Source.fromInputStream(prefixStream)
      val prefixLines = prefixSource.getLines

      val prefixMap = prefixLines.map(_.split(',')).map {
        case Array(id, namespace) => (id, namespace.drop(1).dropRight(1))
      }.toMap

      val validPrefixes = prefixMap.filter {
        case (id, namespace) => !namespace.isEmpty
      }

      Prefixes(validPrefixes)
    } catch {
      case ex: Exception => {
        logger.log(Level.WARNING, "Error loading prefix table.", ex)
        Prefixes.empty
      }
    }
  }
}