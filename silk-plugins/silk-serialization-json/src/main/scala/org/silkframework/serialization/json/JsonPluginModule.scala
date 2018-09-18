package org.silkframework.serialization.json

import org.silkframework.runtime.plugin.PluginModule
import org.silkframework.serialization.json.EntitySerializers.{EntityHolderJsonFormat, EntitySchemaJsonFormat, PairEntitySchemaJsonFormat}
import org.silkframework.serialization.json.InputJsonSerializer.InputJsonFormat
import org.silkframework.serialization.json.JsonSerializers.{GenericInfoJsonFormat, JsonDatasetSpecFormat, MappingRulesJsonFormat, RootMappingRuleJsonFormat, TransformRuleJsonFormat, TransformSpecJsonFormat, TransformTaskJsonFormat, VocabularyPropertyJsonFormat, _}
import org.silkframework.serialization.json.LinkingSerializers.LinkingJsonFormat
import org.silkframework.serialization.json.WorkflowSerializers.WorkflowJsonFormat

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
      WorkflowJsonFormat.getClass ::
      classOf[WorkflowExecutionReportJsonFormat] ::
      EntitySchemaJsonFormat.getClass ::
      PairEntitySchemaJsonFormat.getClass ::
      EntityHolderJsonFormat.getClass ::
      LinkingJsonFormat.getClass ::
      Nil
}
