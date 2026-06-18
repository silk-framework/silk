package org.silkframework.workspace.activity

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.plugin.AnyPlugin
import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.util.Identifier
import org.silkframework.workspace.ProjectTask

@PluginType()
trait ActivityLimit extends AnyPlugin {

  def limitFor(task: Option[ProjectTask[_ <: TaskSpec]], factory: WorkspaceActivityFactory): Option[Int]

  def limiterKey(projectId: Option[Identifier], taskId: Option[Identifier]): ActivityLimiterKey

  def waitingMessage(task: Option[ProjectTask[_ <: TaskSpec]]): String = "Waiting"
}

final case class ActivityLimiterKey(projectId: Option[Identifier],
                                    taskId: Option[Identifier],
                                    limitId: String)
