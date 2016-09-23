package org.silkframework.serialization.json

import org.silkframework.runtime.plugin.PluginModule
import org.silkframework.serialization.json.JsonSerializers.JsonDatasetTaskFormat

class JsonPluginModule extends PluginModule {

  override def pluginClasses: Seq[Class[_]] = JsonDatasetTaskFormat.getClass :: Nil
}
