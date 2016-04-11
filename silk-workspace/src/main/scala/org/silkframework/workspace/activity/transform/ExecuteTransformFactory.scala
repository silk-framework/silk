package org.silkframework.workspace.activity.transform

import org.silkframework.config.TransformSpecification
import org.silkframework.execution.{ExecuteTransformResult, ExecuteTransform}
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.TaskActivityFactory
import org.silkframework.workspace.activity.transform.TransformTaskUtils._

@Plugin(
  id = "ExecuteTransform",
  label = "Execute Transform",
  categories = Array("TransformSpecification"),
  description = "Executes the transformation."
)
case class ExecuteTransformFactory() extends TaskActivityFactory[TransformSpecification, ExecuteTransform] {

  def apply(task: Task[TransformSpecification]): Activity[ExecuteTransformResult] = {
    Activity.regenerating {
      new ExecuteTransform(
        input = task.dataSource,
        selection = task.data.selection,
        rules = task.data.rules,
        outputs = task.entitySinks
      )
    }
  }
}
