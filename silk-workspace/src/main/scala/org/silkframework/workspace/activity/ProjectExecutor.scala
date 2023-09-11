package org.silkframework.workspace.activity

import org.silkframework.runtime.activity.HasValue
import org.silkframework.runtime.plugin.annotations.PluginType

import scala.reflect.ClassTag

/**
  * A project activity factory whose purpose is to execute a project.
  */
@PluginType()
abstract class ProjectExecutor[ActivityType <: HasValue : ClassTag] extends ProjectActivityFactory[ActivityType]
