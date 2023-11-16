package org.silkframework.plugins.dataset.xml

import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}

class XmlPlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] = Seq(
    classOf[XmlDataset],
    classOf[XmlParserTask],
    classOf[LocalXmlParserTaskExecutor],
    classOf[XSLTOperator],
    classOf[LocalXSLTOperatorExecutor],
    classOf[ValidateXsdOperator],
    classOf[LocalValidateXsdOperatorExecutor]
  )
}
