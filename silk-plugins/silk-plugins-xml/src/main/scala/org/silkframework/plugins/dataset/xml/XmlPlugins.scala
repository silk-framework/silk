package org.silkframework.plugins.dataset.xml

import org.silkframework.runtime.plugin.PluginModule

class XmlPlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_]] = Seq(
    classOf[XmlDataset],
    classOf[XmlParserTask],
    classOf[LocalXmlParserTaskExecutor],
    classOf[XSLTOperator],
    classOf[LocalXSLTOperatorExecutor]
  )
}
