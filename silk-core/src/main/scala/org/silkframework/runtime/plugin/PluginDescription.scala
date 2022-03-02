package org.silkframework.runtime.plugin

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
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
  def apply(parameterValues: Map[String, String] = Map.empty, ignoreNonExistingParameters: Boolean = true)
           (implicit prefixes: Prefixes, resources: ResourceManager = EmptyResourceManager()): T


  /**
    * Retrieves the parameters values of a given plugin instance.
    */
  def parameterValues(plugin: AnyRef)(implicit prefixes: Prefixes): Map[String, String] = {
    parameters.map(param => (param.name, param.stringValue(plugin))).toMap
  }

}

object PluginDescription {

  /** Returns a plugin description for a given task. */
  def forTask(task: Task[_ <: TaskSpec]): PluginDescription[_] = {
    task.data match {
      case plugin: AnyPlugin => plugin.pluginSpec
      case DatasetSpec(plugin, _) => plugin.pluginSpec
      case plugin: TaskSpec => ClassPluginDescription(plugin.getClass)
    }
  }

}