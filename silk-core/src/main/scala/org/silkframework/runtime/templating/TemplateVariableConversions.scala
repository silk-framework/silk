package org.silkframework.runtime.templating

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.Entity
import org.silkframework.runtime.plugin.{ParameterValues, PluginContext, PluginObjectParameterTypeTrait, StringParameterType}

object TemplateVariableConversions {

  /**
   * Converts an entity to a sequence of template variables.
   *
   * @param entity The entity to convert.
   * @param scope  The scope to assign to all resulting variables.
   */
  def fromEntity(entity: Entity, scope: Seq[String] = Seq.empty): Seq[TemplateVariableValue] = {
    for((path, value) <- entity.schema.typedPaths zip entity.values if value.nonEmpty) yield {
      new TemplateVariableValue(path.normalizedSerialization, scope, value)
    }
  }

  /**
   * Converts a task's parameters to a sequence of template variables.
   * Nested plugin parameters are placed into nested scopes using the parameter key.
   *
   * @param task  The task whose parameters to convert.
   * @param scope The base scope. Nested parameters extend this scope with the parameter key.
   */
  def fromTask(task: Task[_ <: TaskSpec], scope: Seq[String] = Seq.empty)(implicit pluginContext: PluginContext): Seq[TemplateVariableValue] = {
    fromPluginParameters(task.data.parameters, scope)
  }

  private def fromPluginParameters(values: ParameterValues, scope: Seq[String] = Seq.empty)(implicit pluginContext: PluginContext): Seq[TemplateVariableValue] = {
    for((key, value) <- values.values) yield {
      value match {
        case _: StringParameterType[_] =>
          Seq(new TemplateVariableValue(key, scope, Seq(value.toString)))
        case pt: PluginObjectParameterTypeTrait =>
          fromPluginParameters(pt.parameters, scope :+ key)
      }
    }
  }.flatten.toSeq
}
