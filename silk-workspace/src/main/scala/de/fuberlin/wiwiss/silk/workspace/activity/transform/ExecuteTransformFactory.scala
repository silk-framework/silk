package de.fuberlin.wiwiss.silk.workspace.activity.transform

import de.fuberlin.wiwiss.silk.config.TransformSpecification
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.execution.ExecuteTransform
import de.fuberlin.wiwiss.silk.runtime.activity.Activity
import de.fuberlin.wiwiss.silk.workspace.Task
import de.fuberlin.wiwiss.silk.workspace.activity.TaskActivityFactory

class ExecuteTransformFactory extends TaskActivityFactory[TransformSpecification, ExecuteTransform, Unit] {

  def apply(task: Task[TransformSpecification]): Activity[Unit] = {
    Activity.regenerating {
      new ExecuteTransform(
        input = task.project.task[Dataset](task.data.selection.datasetId).data.source,
        selection = task.data.selection,
        rules = task.data.rules,
        outputs = task.data.outputs.map(id => task.project.task[Dataset](id).data.sink)
      )
    }
  }
}
