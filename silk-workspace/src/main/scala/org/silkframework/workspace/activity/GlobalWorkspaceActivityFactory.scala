package org.silkframework.workspace.activity

import org.silkframework.runtime.activity.{Activity, HasValue}

import scala.reflect.ClassTag

/**
  * Base class for all global workspace activity factories.
  */
abstract class GlobalWorkspaceActivityFactory[ActivityType <: HasValue : ClassTag]
    extends WorkspaceActivityFactory {
  def apply(): Activity[ActivityType#ValueType]
  /**
    * Returns the type of generated activity.
    */
  def activityType: Class[_] = implicitly[ClassTag[ActivityType]].runtimeClass

  /** True, if this activity shall be executed automatically after startup */
  def autoRun: Boolean = false
}
