package org.silkframework.plugins.dataset.text

import org.silkframework.runtime.plugin.PluginModule

class TextPlugins extends PluginModule {

  override def pluginClasses = Seq(classOf[TextFileDataset])

}