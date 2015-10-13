package de.fuberlin.wiwiss.silk.plugins.dataset.xml

import de.fuberlin.wiwiss.silk.runtime.plugin.PluginModule

class XmlPlugins extends PluginModule {

  override def pluginClasses = Seq(classOf[XmlDataset])

}
