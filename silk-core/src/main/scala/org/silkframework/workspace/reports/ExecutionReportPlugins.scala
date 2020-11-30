package org.silkframework.workspace.reports

import org.silkframework.runtime.plugin.PluginModule

class ExecutionReportPlugins extends PluginModule {
  override def pluginClasses: Seq[Class[_]] = Seq(classOf[EmptyExecutionReportManager], classOf[InMemoryExecutionReportManager])
}
