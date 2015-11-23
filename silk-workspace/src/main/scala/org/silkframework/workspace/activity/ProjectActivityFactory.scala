package org.silkframework.workspace.activity

import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.AnyPlugin
import org.silkframework.workspace.Project

import scala.reflect.ClassTag

abstract class ProjectActivityFactory[ActivityType <: Activity[Unit] : ClassTag] extends AnyPlugin with (Project => Activity[Unit]) {

  def apply(project: Project): Activity[Unit]

  /**
    * Returns the type of generated activity.
    */
  def activityType = implicitly[ClassTag[ActivityType]].runtimeClass

}
