package org.silkframework.serialization.json

import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}
import org.silkframework.serialization.json.EntitySerializers.{EntityHolderJsonFormat, EntitySchemaJsonFormat, PairEntitySchemaJsonFormat}
import org.silkframework.serialization.json.ExecutionReportSerializers._
import org.silkframework.serialization.json.InputJsonSerializer.{CachedEntitySchemataJsonFormat, InputJsonFormat}
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.serialization.json.LinkingSerializers.ReferenceLinksJsonFormat
import org.silkframework.serialization.json.PluginDescriptionSerializers.PluginListJsonFormat
import org.silkframework.serialization.json.WorkflowSerializers.{TaskIdentifierParameterFormat, WorkflowDatasetsParameterFormat, WorkflowJsonFormat, WorkflowOperatorsParameterFormat}

class JsonPluginModule extends PluginModule {

  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] =
      StringJsonFormat.getClass ::
      PluginListJsonFormat.getClass ::
      TaskSpecJsonFormat.getClass ::
      GenericTaskJsonFormat.getClass ::
      DatasetSpecJsonFormat.getClass ::
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
      VocabularyCacheValueJsonFormat.getClass ::
      VocabularyClassJsonFormat.getClass ::
      InputJsonFormat.getClass ::
      WorkflowJsonFormat.getClass ::
      WorkflowOperatorsParameterFormat.getClass ::
      WorkflowDatasetsParameterFormat.getClass ::
      ExecutionReportJsonFormat.getClass ::
      TransformReportJsonFormat.getClass ::
      WorkflowExecutionReportJsonFormat.getClass ::
      WorkflowExecutionReportWithProvenanceJsonFormat.getClass ::
      EntitySchemaJsonFormat.getClass ::
      PairEntitySchemaJsonFormat.getClass ::
      CachedEntitySchemataJsonFormat.getClass ::
      EntityHolderJsonFormat.getClass ::
      LinkingJsonFormat.getClass ::
      TransformReportJsonFormat.getClass ::
      LinkageRuleJsonFormat.getClass ::
      DatasetSelectionJsonFormat.getClass ::
      ReferenceLinksJsonFormat.getClass ::
      ComplexMappingJsonFormat.getClass ::
      UiAnnotationsJsonFormat.getClass ::
      TaskIdentifierParameterFormat.getClass ::
      classOf[InputTaskJsonTransformer] ::
      Nil
}
