package org.silkframework.workspace.activity

import org.silkframework.runtime.activity.{Activity, ActivityControl, HasValue}
import org.silkframework.workspace.{Project, ProjectTask}
import scala.reflect.ClassTag

class ProjectActivity[ActivityType <: HasValue : ClassTag](override val project: Project, defaultFactory: ProjectActivityFactory[ActivityType])
  extends WorkspaceActivity[ActivityType] {

  override def taskOption: Option[ProjectTask[_]] = None

  override def factory: ProjectActivityFactory[ActivityType] = defaultFactory

  override protected def createInstance(config: Map[String, String]): ActivityControl[ActivityType#ValueType] = {
    Activity(defaultFactory(project))
  }
}
