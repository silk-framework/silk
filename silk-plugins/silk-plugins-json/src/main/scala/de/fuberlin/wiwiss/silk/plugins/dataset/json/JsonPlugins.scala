package de.fuberlin.wiwiss.silk.plugins.dataset.json

import de.fuberlin.wiwiss.silk.runtime.plugin.PluginModule

class JsonPlugins extends PluginModule {

  override def pluginClasses = Seq(classOf[JsonDataset])

}
