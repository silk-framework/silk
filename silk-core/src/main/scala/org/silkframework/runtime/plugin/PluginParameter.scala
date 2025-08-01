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
  */
trait PluginParameter {

  /** The parameter name */
  def name: String

  /** The type of the parameter  */
  def parameterType: ParameterType[_]

  /** A human-readable label of the parameter  */
  def label: String

  /** A human-readable description of the parameter  */
  def description: String = "No description"

  /** The default value, if any  */
  def defaultValue: Option[AnyRef] = None

  /** An example value for this parameter */
  def exampleValue: Option[AnyRef] = None

  /** Is this an advanced parameter that should only be changed by experienced users. */
  def advanced: Boolean = false

  /**  */
  def visibleInDialog: Boolean = true

  /** True, if it can be edited in the UI plugin dialogs. */
  def autoCompletion: Option[ParameterAutoCompletion] = None

  /**
    * Retrieves the current value of this parameter.
    */
  def apply(obj: AnyRef): AnyRef

  /**
    * Retrieves the current value of this parameter as string.
    */
  def stringValue(obj: AnyRef)(implicit pluginContext: PluginContext): String = {
    formatValue(apply(obj))
  }

  /**
    * Returns the default value as string.
    */
  def stringDefaultValue(implicit pluginContext: PluginContext): Option[String] = {
    for(value <- defaultValue) yield formatValue(value)
  }

  /**
   * Formats a parameter value as string.
   */
  private def formatValue(value: AnyRef)(implicit pluginContext: PluginContext): String = {
    parameterType.asInstanceOf[ParameterType[AnyRef]].toString(value)
  }

}

/**
  * A class plugin parameter.
  *
  * @param name         The parameter name as used by the plugin class
  * @param parameterType     The type of the parameter
  * @param label        A human-readable label of the parameter
  * @param description  A human-readable description of the parameter
  * @param defaultValue The default value, if any
  * @param exampleValue An example value for this parameter
  * @param advanced     Is this an advanced parameter that should only be changed by experienced users.
  * @param visibleInDialog True, if it can be edited in the UI plugin dialogs.
  * @param autoCompletion The parameter auto-completion object.
  */
case class ClassPluginParameter(override val name: String,
                                override val parameterType: ParameterType[_],
                                override val label: String,
                                override val description: String = "No description",
                                override val defaultValue: Option[AnyRef] = None,
                                override val exampleValue: Option[AnyRef] = None,
                                override val advanced: Boolean = false,
                                override val visibleInDialog: Boolean = true,
                                override val autoCompletion: Option[ParameterAutoCompletion] = None) extends PluginParameter {

  /**
    * Retrieves the current value of this parameter.
    */
  override def apply(obj: AnyRef): AnyRef = {
    obj.getClass.getMethod(name).invoke(obj)
  }
}

case class ParameterAutoCompletion(autoCompletionProvider: PluginParameterAutoCompletionProvider,
                                   allowOnlyAutoCompletedValues: Boolean = false,
                                   autoCompleteValueWithLabels: Boolean = false,
                                   autoCompletionDependsOnParameters: Seq[String] = Seq.empty)