package org.silkframework.plugins.dataset.json

import org.silkframework.runtime.plugin.PluginModule

class JsonPlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_]] =
    classOf[JsonDataset] ::
        classOf[JsonParserTask] ::
        classOf[LocalJsonParserTaskExecutor] ::
        Nil

}
