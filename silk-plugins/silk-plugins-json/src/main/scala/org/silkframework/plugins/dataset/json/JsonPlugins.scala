package org.silkframework.plugins.dataset.json

import org.silkframework.runtime.plugin.PluginModule

class JsonPlugins extends PluginModule {

  override def pluginClasses = Seq(classOf[JsonDataset])

}
