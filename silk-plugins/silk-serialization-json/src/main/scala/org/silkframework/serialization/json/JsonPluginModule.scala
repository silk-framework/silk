package org.silkframework.serialization.json

import org.silkframework.runtime.plugin.PluginModule
import org.silkframework.serialization.json.JsonSerializers.{JsonDatasetTaskFormat, MappingRulesJsonFormat, TransformRuleJsonFormat, TransformSpecJsonFormat, TransformTaskFormat}

class JsonPluginModule extends PluginModule {

  override def pluginClasses: Seq[Class[_]] =
      JsonDatasetTaskFormat.getClass ::
      TransformSpecJsonFormat.getClass ::
      TransformRuleJsonFormat.getClass ::
      MappingRulesJsonFormat.getClass ::
      TransformTaskFormat.getClass ::
      Nil
}
