package org.silkframework.workbench.workflow

import org.silkframework.runtime.plugin.PluginModule

class WorkflowPlugins extends PluginModule {
  override def pluginClasses: Seq[Class[_]] = Seq(classOf[WorkflowPlugin])
}
