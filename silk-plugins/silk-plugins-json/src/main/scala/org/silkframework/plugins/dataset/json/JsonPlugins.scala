package org.silkframework.plugins.dataset.json

import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}

class JsonPlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] =
    Seq(
      classOf[JsonDataset],
      classOf[JsonDatasetExecutor],
      classOf[JsonParserTask],
      classOf[LocalJsonParserTaskExecutor],
      classOf[JsonToFileOperator],
      classOf[LocalJsonToFileOperatorExecutor]
    )

}
