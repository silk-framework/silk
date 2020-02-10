package org.silkframework.workspace.activity.transform

import org.silkframework.dataset.CombinedEntitySink
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.{ExecuteTransform, TransformReport}
import org.silkframework.runtime.activity.{Activity, UserContext}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory
import org.silkframework.workspace.activity.transform.TransformTaskUtils._

@Plugin(
  id = ExecuteTransformFactory.pluginId,
  label = "Execute Transform",
  categories = Array("TransformSpecification"),
  description = "Executes the transformation."
)
case class ExecuteTransformFactory(
                                   @Param("Limits the maximum number of entities that are transformed.")
                                   limit: Option[Int] = None) extends TaskActivityFactory[TransformSpec, ExecuteTransform] {

  override def apply(task: ProjectTask[TransformSpec]): Activity[TransformReport] = {
    Activity.regenerating {
      new ExecuteTransform(
        task.taskLabel(),
        // No user context here, defer fetching data sources
        (userContext: UserContext) => task.dataSource(userContext),
        task.data,
        (userContext: UserContext) => new CombinedEntitySink(task.entitySinks(userContext)),
        limit
      )(task.project.config.prefixes)
    }
  }
}

object ExecuteTransformFactory {

  final val pluginId = "ExecuteTransform"

}