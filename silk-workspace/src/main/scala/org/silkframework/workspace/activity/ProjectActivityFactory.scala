package org.silkframework.workspace.activity

import org.silkframework.runtime.activity.{Activity, HasValue}
import org.silkframework.workspace.Project

import scala.reflect.ClassTag

abstract class ProjectActivityFactory[ActivityType <: HasValue : ClassTag]
  extends WorkspaceActivityFactory with (Project => Activity[ActivityType#ValueType]) {

  def apply(project: Project): Activity[ActivityType#ValueType]

  /**
    * Returns the type of generated activity.
    */
  def activityType: Class[_] = implicitly[ClassTag[ActivityType]].runtimeClass

}
