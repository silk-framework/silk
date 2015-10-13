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

package de.fuberlin.wiwiss.silk.dataset

import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.runtime.plugin.{PluginFactory, AnyPlugin}

/**
 * Serializes a link.
 */
trait Formatter extends AnyPlugin {
  def header: String = ""

  def footer: String = ""

  def format(link: Link, predicate: String): String

  def formatLiteralStatement(subject: String, predicate: String, value: String): String = ???
}

/**
 * Formatter factory
 */
object Formatter extends PluginFactory[Formatter]
