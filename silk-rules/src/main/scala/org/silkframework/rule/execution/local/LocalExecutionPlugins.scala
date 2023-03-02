package org.silkframework.rule.execution.local

import org.silkframework.execution.local.GenericLocalDatasetExecutor
import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}

/**
  * Created on 7/21/16.
  */
class LocalExecutionPlugins extends PluginModule {
  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] = executors

  def executors = List(
    classOf[GenericLocalDatasetExecutor],
    classOf[LocalLinkSpecExecutor],
    classOf[LocalTransformSpecExecutor]
  )
}
