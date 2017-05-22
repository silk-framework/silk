package org.silkframework.serialization.json

import org.silkframework.runtime.plugin.PluginModule
import org.silkframework.serialization.json.JsonSerializers.{GenericInfoJsonFormat, JsonDatasetTaskFormat, TransformRuleJsonFormat, VocabularyPropertyJsonFormat}

class JsonPluginModule extends PluginModule {

  override def pluginClasses: Seq[Class[_]] = JsonDatasetTaskFormat.getClass ::
      TransformRuleJsonFormat.getClass ::
      VocabularyPropertyJsonFormat.getClass ::
      GenericInfoJsonFormat.getClass ::
      Nil
}
