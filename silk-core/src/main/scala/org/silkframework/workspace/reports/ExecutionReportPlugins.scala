package org.silkframework.workspace.reports

import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}

class ExecutionReportPlugins extends PluginModule {
  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] = Seq(classOf[EmptyExecutionReportManager], classOf[InMemoryExecutionReportManager])
}
