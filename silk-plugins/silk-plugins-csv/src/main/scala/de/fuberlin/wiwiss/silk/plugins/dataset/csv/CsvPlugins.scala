package de.fuberlin.wiwiss.silk.plugins.dataset.csv

import de.fuberlin.wiwiss.silk.runtime.plugin.PluginModule

class CsvPlugins extends PluginModule {

  override def pluginClasses = Seq(classOf[CsvDataset])

}
