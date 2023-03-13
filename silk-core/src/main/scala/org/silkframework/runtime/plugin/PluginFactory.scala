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

package org.silkframework.runtime.plugin

import org.silkframework.config.Prefixes

import scala.reflect.ClassTag

/**
 * Companion objects may inherit from this class to offer convenience methods for instantiating plugins.
 */
class PluginFactory[T <: AnyPlugin: ClassTag] {

  /**
   * Creates a new instance of a specific plugin.
   */
  def apply(id: String,
            params: ParameterValues = ParameterValues.empty)
           (implicit context: PluginContext): T = {
    PluginRegistry.create(id, params)
  }

  /**
   * Retrieves the parameters of a plugin instance e.g. to serialize it.
   */
  def unapply(t: T)(implicit prefixes: Prefixes = Prefixes.empty): Option[(PluginDescription[_], ParameterValues)] = {
    Some(t.pluginSpec, t.parameters)
  }

  /**
   * List of all registered plugins.
   */
  def availablePlugins: Seq[PluginDescription[_]] = PluginRegistry.availablePlugins[T]

  /**
   * A map from each category to all corresponding plugins
   */
  def pluginsByCategory: Map[String, Seq[PluginDescription[_]]] = {
    PluginRegistry.pluginsByCategory[T]
  }

  /**
   * Registers a single plugin.
   */
  def register(implementationClass: Class[_ <: T]) {
    PluginRegistry.registerPlugin(implementationClass)
  }
}
