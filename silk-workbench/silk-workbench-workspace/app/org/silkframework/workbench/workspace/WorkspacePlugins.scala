package org.silkframework.workbench.workspace

import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}

class WorkspacePlugins extends PluginModule {
  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] =
    Seq(
      classOf[WorkbenchPluginDataset],
      classOf[WorkbenchPluginCustomTask],
      classOf[FileExecutionReportManager])
}
