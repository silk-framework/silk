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
import org.silkframework.dataset.DatasetSpec
import org.silkframework.runtime.validation.ValidationException

/**
 * Plugin interface.
 */
trait AnyPlugin {

  /**
   * The description for this plugin.
   */
  @transient lazy val pluginSpec = PluginDescription(getClass)

  /**
   * The parameters for this plugin as Map.
   */
  @transient lazy val parameters: Map[String, String] = pluginSpec.parameterValues(this)(Prefixes.empty)

  /**
    * Creates a new instance of this plugin with updated properties.
    *
    * @param updatedProperties A list of property values to be updated.
    *                          This can be a subset of all available properties.
    *                          Property values that are not part of the map remain unchanged.
    */
  def withParameters(updatedProperties: Map[String, String])(implicit prefixes: Prefixes): this.type = {
    val invalidParameters = updatedProperties.keySet -- parameters.keySet
    if(invalidParameters.nonEmpty) {
      throw new ValidationException(s"The following properties cannot be updated on plugin $this because they are no valid parameters: $invalidParameters")
    }

    val updatedParameters = parameters ++ updatedProperties
    pluginSpec.apply(updatedParameters).asInstanceOf[this.type]
  }

  override def toString: String = {
    getClass.getSimpleName + "(" + parameters.map { case (key, value) => key + "=" + value }.mkString(" ") + ")"
  }
}
