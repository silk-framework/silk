package org.silkframework.workbench

import org.silkframework.runtime.plugin.PluginModule

class WorkbenchCorePlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_]] = Seq(classOf[PluginListCsvFormat])
}
