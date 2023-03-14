package org.silkframework.plugins.dataset.json

import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}

class JsonPlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] =
    classOf[JsonDataset] ::
     classOf[JsonParserTask] ::
     classOf[LocalJsonParserTaskExecutor] ::
     Nil

}
