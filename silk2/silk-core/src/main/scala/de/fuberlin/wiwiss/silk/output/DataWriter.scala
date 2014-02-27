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

package de.fuberlin.wiwiss.silk.output

import de.fuberlin.wiwiss.silk.runtime.plugin.{PluginFactory, AnyPlugin}
import de.fuberlin.wiwiss.silk.entity.Link

/**
 * Represents an abstraction over an writer of links.
 *
 * Implementing classes of this trait must override the write method.
 */
trait DataWriter extends AnyPlugin {
  /**
   * Initializes this writer.
   */
  def open() {}

  /**
   * Writes a new link to this writer.
   */
  def write(link: Link, predicateUri: String)

  def writeLiteralStatement(subject: String, predicate: String, value: String)

  /**
   * Closes this writer.
   */
  def close() {}
}

object DataWriter extends PluginFactory[DataWriter]