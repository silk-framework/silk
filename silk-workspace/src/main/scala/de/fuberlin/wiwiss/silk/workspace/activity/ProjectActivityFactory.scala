package de.fuberlin.wiwiss.silk.workspace.activity

import de.fuberlin.wiwiss.silk.runtime.activity.Activity
import de.fuberlin.wiwiss.silk.runtime.plugin.AnyPlugin
import de.fuberlin.wiwiss.silk.workspace.Project

import scala.reflect.ClassTag

abstract class ProjectActivityFactory[ActivityType <: Activity[Unit] : ClassTag] extends AnyPlugin with (Project => Activity[Unit]) {

  def apply(project: Project): Activity[Unit]

  /**
    * Returns the type of generated activity.
    */
  def activityType = implicitly[ClassTag[ActivityType]].runtimeClass

}
