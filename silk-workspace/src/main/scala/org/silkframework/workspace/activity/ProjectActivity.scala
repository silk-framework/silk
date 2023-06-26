package org.silkframework.workspace.activity

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.{Activity, ActivityControl, HasValue, ProjectAndTaskIds}
import org.silkframework.runtime.plugin.ParameterValues
import org.silkframework.workspace.{Project, ProjectTask}

import scala.reflect.ClassTag

class ProjectActivity[ActivityType <: HasValue : ClassTag](val project: Project, defaultFactory: ProjectActivityFactory[ActivityType])
  extends WorkspaceActivity[ActivityType] {

  override def taskOption: Option[ProjectTask[_ <: TaskSpec]] = None

  override def projectOpt: Option[Project] = Some(project)

  override def factory: ProjectActivityFactory[ActivityType] = defaultFactory

  override protected def createInstanceFromParameterValues(config: ParameterValues): ActivityControl[ActivityType#ValueType] = {
    Activity(defaultFactory(project), projectAndTaskId = Some(ProjectAndTaskIds(project.id, None)))
  }
}
