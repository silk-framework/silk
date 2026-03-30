package org.silkframework.runtime.templating

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.Entity

object TemplateVariableConversions {

  /**
   * Converts an entity to a sequence of template variables.
   */
  def fromEntity(entity: Entity, scope: String = ""): Seq[TemplateVariableValue] = {
    for((path, value) <- entity.schema.typedPaths zip entity.values if value.nonEmpty) yield {
      new TemplateVariableValue(path.normalizedSerialization, scope, value)
    }
  }

  def fromTask(task: Task[_ <: TaskSpec]): Seq[TemplateVariableValue] = {

  }

}
