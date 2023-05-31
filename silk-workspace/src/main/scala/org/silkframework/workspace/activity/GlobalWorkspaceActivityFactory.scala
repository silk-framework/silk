package org.silkframework.workspace.activity

import org.silkframework.runtime.activity.{Activity, HasValue}
import org.silkframework.runtime.plugin.annotations.PluginType

import scala.reflect.ClassTag

/**
  * Base class for all global workspace activity factories.
  */
@PluginType()
abstract class GlobalWorkspaceActivityFactory[ActivityType <: HasValue : ClassTag]
    extends WorkspaceActivityFactory {
  def apply(): Activity[ActivityType#ValueType]
  /**
    * Returns the type of generated activity.
    */
  override def activityType: Class[_] = implicitly[ClassTag[ActivityType]].runtimeClass

  /** True, if this activity shall be executed automatically after startup */
  def autoRun: Boolean = false
}
