package org.silkframework.workspace.activity.transform

import org.silkframework.dataset.CombinedEntitySink
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.{ExecuteTransform, TransformReport}
import org.silkframework.runtime.activity.{Activity, UserContext}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory
import org.silkframework.workspace.activity.transform.TransformTaskUtils._

@Plugin(
  id = ExecuteTransformFactory.pluginId,
  label = "Execute transform",
  categories = Array("TransformSpecification"),
  description = "Executes the transformation."
)
case class ExecuteTransformFactory() extends TaskActivityFactory[TransformSpec, ExecuteTransform] {

  override def apply(task: ProjectTask[TransformSpec]): Activity[TransformReport] = {
    Activity.regenerating {
      new ExecuteTransform(
        task,
        // No user context here, defer fetching data sources
        (userContext: UserContext) => task.project.anyTask(task.selection.inputId)(userContext),
        (userContext: UserContext) => task.dataSource(userContext),
        (userContext: UserContext) => new CombinedEntitySink(task.entitySink(userContext).toSeq),
        (userContext: UserContext) => task.errorEntitySink(userContext),
        (userContext: UserContext) => PluginContext.fromProject(task.project)(userContext)
      )
    }
  }
}

object ExecuteTransformFactory {

  final val pluginId = "ExecuteTransform"

}