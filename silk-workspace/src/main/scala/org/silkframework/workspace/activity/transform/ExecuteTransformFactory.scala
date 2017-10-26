package org.silkframework.workspace.activity.transform

import org.silkframework.dataset.CombinedEntitySink
import org.silkframework.rule.execution.{ExecuteTransform, TransformReport}
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory
import org.silkframework.workspace.activity.transform.TransformTaskUtils._

@Plugin(
  id = "ExecuteTransform",
  label = "Execute Transform",
  categories = Array("TransformSpecification"),
  description = "Executes the transformation."
)
case class ExecuteTransformFactory() extends TaskActivityFactory[TransformSpec, ExecuteTransform] {

  def apply(task: ProjectTask[TransformSpec]): Activity[TransformReport] = {
    Activity.regenerating {
      new ExecuteTransform(
        task.dataSource,
        task.data,
        new CombinedEntitySink(task.entitySinks)
      )
    }
  }
}
