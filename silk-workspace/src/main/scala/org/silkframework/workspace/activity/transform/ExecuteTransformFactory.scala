package org.silkframework.workspace.activity.transform

import org.silkframework.config.TransformSpecification
import org.silkframework.dataset.Dataset
import org.silkframework.execution.ExecuteTransform
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.TaskActivityFactory

@Plugin(
  id = "ExecuteTransform",
  label = "Execute Transform",
  categories = Array("TransformSpecification"),
  description = "Executes the transformation."
)
case class ExecuteTransformFactory() extends TaskActivityFactory[TransformSpecification, ExecuteTransform] {

  def apply(task: Task[TransformSpecification]): Activity[Unit] = {
    Activity.regenerating {
      new ExecuteTransform(
        input = task.project.task[Dataset](task.data.selection.inputId).data.source,
        selection = task.data.selection,
        rules = task.data.rules,
        outputs = task.data.outputs.map(id => task.project.task[Dataset](id).data.entitySink)
      )
    }
  }
}
