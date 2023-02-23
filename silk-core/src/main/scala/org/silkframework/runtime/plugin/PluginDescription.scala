package org.silkframework.runtime.plugin

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.util.Identifier

/**
  * Describes a plugin.
  *
  * @tparam T The plugin base class.
  */
trait PluginDescription[+T] {

  /** The id of this plugin */
  val id: Identifier

  /** The categories to which this plugin belongs to. */
  val categories: Seq[String]

  /** A human-readable label.  */
  val label: String

  /** A short (few sentence) description of this plugin. */
  val description: String

  /** Documentation for this plugin in Markdown. */
  val documentation: String

  /** The parameters of the plugin class.  */
  val parameters: Seq[PluginParameter]

  /** The concrete plugin class  */
  def pluginClass: Class[_ <: T]

  /**
    * Creates an instance of this plugin with the given parameters.
    *
    * @param parameterValues The parameter values to be used for instantiation. Will override any default.
    * @param ignoreNonExistingParameters If true, parameter values for parameters that do not exist are ignored.
    *                                   If false, creation will fail if a parameter is provided that does not exist on the plugin.
    */
  def apply(parameterValues: ParameterValues = ParameterValues.empty,
            ignoreNonExistingParameters: Boolean = true)
           (implicit context: PluginContext): T


  /**
    * Retrieves the parameters values of a given plugin instance.
    */
  def parameterValues(plugin: AnyPlugin)(implicit prefixes: Prefixes): ParameterValues = {
    val values =
      for(param <- parameters) yield {
        val value =
          param.parameterType match {
            case _: StringParameterType[_] =>
              plugin.templateValues.get(param.name) match {
                case Some(templateValue) =>
                  ParameterTemplateValue(templateValue)
                case None =>
                  ParameterStringValue(param.stringValue(plugin))
              }
            case pt: PluginObjectParameterTypeTrait =>
              pt.pluginDescription match {
                case Some(pd) =>
                  pd.parameterValues(param(plugin).asInstanceOf[AnyPlugin])
                case None =>
                  ParameterObjectValue(param(plugin))
              }
          }
        (param.name, value)
      }
    ParameterValues(values.toMap)
  }

}

object PluginDescription {

  /** Returns a plugin description for a given task. */
  def forTask(task: Task[_ <: TaskSpec]): PluginDescription[_] = {
    task.data match {
      case plugin: AnyPlugin => plugin.pluginSpec
      case DatasetSpec(plugin, _) => plugin.pluginSpec
      case _ => throw new IllegalArgumentException(s"Provided task $task is not a plugin.")
    }
  }

}