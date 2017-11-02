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

import scala.language.existentials

/**
  * A plugin parameter.
  *
  * @param name The parameter name as used by the plugin class
  * @param dataType The type of the parameter
  * @param label A human-readable label of the parameter
  * @param description A human-readable description of the parameter
  * @param defaultValue The default value, if any
  * @param exampleValue An example value for this parameter
  */
case class Parameter(name: String,
                     dataType: ParameterType[_],
                     label: String,
                     description: String = "No description",
                     defaultValue: Option[AnyRef] = None,
                     exampleValue: Option[AnyRef] = None) {

  /**
   * Retrieves the current value of this parameter.
   */
  def apply(obj: AnyRef): AnyRef = {
    obj.getClass.getMethod(name).invoke(obj)
  }

  /**
    * Retrieves the current value of this parameter as string.
    */
  def stringValue(obj: AnyRef)(implicit prefixes: Prefixes): String = {
    formatValue(apply(obj))
  }

  def stringDefaultValue(implicit prefixes: Prefixes): Option[String] = {
    for(value <- defaultValue) yield formatValue(value)
  }

  def stringExampleValue(implicit prefixes: Prefixes): Option[String] = {
    for(value <- exampleValue) yield formatValue(value)
  }

  private def formatValue(value: AnyRef)(implicit prefixes: Prefixes) = {
    dataType.asInstanceOf[ParameterType[AnyRef]].toString(value)
  }
}
