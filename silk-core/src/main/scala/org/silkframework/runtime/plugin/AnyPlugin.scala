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

/**
 * Plugin interface.
 */
trait AnyPlugin {

  /**
   * The description for this plugin.
   */
  @transient lazy val pluginSpec: PluginDescription[AnyPlugin] = ClassPluginDescription(getClass)

  /**
   * The parameters for this plugin as Map.
   */
  @transient lazy val parameters: ParameterValues = pluginSpec.parameterValues(this)(Prefixes.empty)

  /**
    * Holds all templates. Set by ClassPluginDescription.
    */
  @volatile
  var templateValues: Map[String, String] = Map.empty

  /**
    * Creates a new instance of this plugin with updated properties.
    *
    * @param updatedParameters A list of property values to be updated.
    *                          This can be a subset of all available properties.
    *                          Property values that are not part of the map remain unchanged.
    */
  def withParameters(updatedParameters: ParameterValues)(implicit context: PluginContext): this.type = {
    pluginSpec(parameters merge updatedParameters, ignoreNonExistingParameters = false).asInstanceOf[this.type]
  }
}
