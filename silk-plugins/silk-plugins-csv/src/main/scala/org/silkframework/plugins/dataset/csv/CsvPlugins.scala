package org.silkframework.plugins.dataset.csv

import org.silkframework.runtime.plugin.PluginModule

class CsvPlugins extends PluginModule {

  override def pluginClasses = Seq(classOf[CsvDataset])

}
