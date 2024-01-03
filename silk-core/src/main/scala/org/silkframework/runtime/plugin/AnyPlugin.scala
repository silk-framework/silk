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

/**
 * Plugin interface.
 */
trait AnyPlugin {

  /**
   * The description for this plugin.
   */
  @transient lazy val pluginSpec: PluginDescription[AnyPlugin] = ClassPluginDescription(getClass)

  /**
    * Holds all templates. Set by ClassPluginDescription.
    */
  @volatile
  var templateValues: Map[String, String] = Map.empty

  /**
    * Retrieves all parameter values for this plugin.
    */
  def parameters(implicit pluginContext: PluginContext): ParameterValues = pluginSpec.parameterValues(this)

  /**
    * Creates a new instance of this plugin with updated parameters.
    *
    * @param updatedParameters A list of parameter values to be updated.
    *                          Parameter values that are not provided remain unchanged.
    * @param dropExistingValues If true, the caller is expected to provide values for all parameters.
    *                           If false, the updated parameters can be a subset of all available parameters.
    */
  def withParameters(updatedParameters: ParameterValues, dropExistingValues: Boolean = false)(implicit context: PluginContext): this.type = {
    val allParameters = if(dropExistingValues) updatedParameters else parameters merge updatedParameters
    pluginSpec(allParameters, ignoreNonExistingParameters = false).asInstanceOf[this.type]
  }
}
