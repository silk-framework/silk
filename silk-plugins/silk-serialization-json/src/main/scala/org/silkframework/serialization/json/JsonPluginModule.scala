package org.silkframework.serialization.json

import org.silkframework.runtime.plugin.PluginModule
import org.silkframework.serialization.json.JsonSerializers.{JsonDatasetSpecFormat, MappingRulesJsonFormat, RootMappingRuleJsonFormat, TransformRuleJsonFormat, TransformSpecJsonFormat, TransformTaskJsonFormat}
import org.silkframework.serialization.json.JsonSerializers.{GenericInfoJsonFormat, JsonDatasetSpecFormat, TransformRuleJsonFormat, VocabularyPropertyJsonFormat}
import org.silkframework.serialization.json.InputJsonSerializer.InputJsonFormat
import org.silkframework.serialization.json.JsonSerializers._

class JsonPluginModule extends PluginModule {

  override def pluginClasses: Seq[Class[_]] =
      TaskSpecJsonFormat.getClass ::
      GenericTaskJsonFormat.getClass ::
      JsonDatasetSpecFormat.getClass ::
      CustomTaskJsonFormat.getClass ::
      TransformSpecJsonFormat.getClass ::
      LinkSpecJsonFormat.getClass ::
      TransformRuleJsonFormat.getClass ::
      MappingRulesJsonFormat.getClass ::
      DatasetTaskJsonFormat.getClass ::
      TransformTaskJsonFormat.getClass ::
      RootMappingRuleJsonFormat.getClass ::
      VocabularyPropertyJsonFormat.getClass ::
      GenericInfoJsonFormat.getClass ::
      MappingTargetJsonFormat.getClass ::
      PathInputJsonFormat.getClass ::
      TransformInputJsonFormat.getClass ::
      ValueTypeJsonFormat.getClass ::
      GenericInfoJsonFormat.getClass ::
      VocabularyClassJsonFormat.getClass ::
      InputJsonFormat.getClass ::
      Nil
}
