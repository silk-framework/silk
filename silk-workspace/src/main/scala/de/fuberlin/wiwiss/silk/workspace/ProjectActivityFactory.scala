package de.fuberlin.wiwiss.silk.workspace

import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityContext, Activity}
import de.fuberlin.wiwiss.silk.runtime.plugin.AnyPlugin

import scala.reflect.ClassTag

abstract class ProjectActivityFactory[ActivityType <: Activity[Unit] : ClassTag] extends AnyPlugin {

  def apply(project: Project): Activity[Unit]

  /**
    * Returns the type of generated activity.
    */
  def activityType = implicitly[ClassTag[ActivityType]].runtimeClass

}
