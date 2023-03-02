package org.silkframework.workbench

import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}

class WorkbenchCorePlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] = Seq(classOf[PluginListCsvFormat])
}
