package org.silkframework.workspace.activity

import org.silkframework.runtime.activity.{Activity, HasValue}
import org.silkframework.runtime.plugin.AnyPlugin
import org.silkframework.workspace.Project

import scala.reflect.ClassTag

abstract class ProjectActivityFactory[ActivityType <: HasValue : ClassTag] extends AnyPlugin with (Project => Activity[ActivityType#ValueType]) {

  def apply(project: Project): Activity[ActivityType#ValueType]

  /**
    * Returns the type of generated activity.
    */
  def activityType = implicitly[ClassTag[ActivityType]].runtimeClass

}
