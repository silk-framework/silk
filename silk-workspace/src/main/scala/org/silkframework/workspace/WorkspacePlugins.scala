package org.silkframework.workspace

import org.silkframework.plugins.dataset.DatasetTypeAutoCompletionProvider
import org.silkframework.plugins.filter.RemoveStopwords
import org.silkframework.plugins.transformer.value.ReadParameter
import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}
import org.silkframework.workspace.activity.dataset.Types.TypesFormat
import org.silkframework.workspace.activity.dataset.TypesCacheFactory
import org.silkframework.workspace.activity.linking._
import org.silkframework.workspace.activity.transform.CachedEntitySchemata.CachedEntitySchemaXmlFormat
import org.silkframework.workspace.activity.transform._
import org.silkframework.workspace.activity.vocabulary.GlobalVocabularyCacheFactory
import org.silkframework.workspace.activity.workflow.TaskIdentifierParameter.TaskIdentifierParameterXmlFormat
import org.silkframework.workspace.activity.workflow.Workflow.WorkflowXmlFormat
import org.silkframework.workspace.activity.workflow.WorkflowDatasetsParameter.WorkflowDatasetsFormat
import org.silkframework.workspace.activity.workflow.WorkflowOperatorsParameter.WorkflowOperatorsFormat
import org.silkframework.workspace.activity.workflow.{LocalWorkflowAsTaskExecutor, LocalWorkflowExecutorFactory, NopPersistWorkflowProvenance, Workflow}
import org.silkframework.workspace.xml.{FileWorkspaceProvider, XmlZipWithResourcesProjectMarshaling, XmlZipWithoutResourcesProjectMarshaling}

import scala.language.existentials

class WorkspacePlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] =
    workspaceProviders :::
    datasetActivities :::
    transformActivities :::
    linkingActivities :::
    workflowActivities :::
    projectMarshaller :::
    provenancePlugins :::
    rulePlugins :::
    workspaceTaskPlugins :::
    autoCompletionProviderPlugins :::
    workspaceActivityPlugins :::
    formats

  def workspaceTaskPlugins: List[Class[_ <: AnyPlugin]] =
    classOf[Workflow] ::
    classOf[LocalWorkflowAsTaskExecutor] ::
    Nil

  def workspaceProviders: List[Class[_ <: AnyPlugin]] =
    classOf[FileWorkspaceProvider] ::
    classOf[InMemoryWorkspaceProvider] :: Nil

  def datasetActivities: List[Class[_ <: AnyPlugin]] =
    classOf[TypesCacheFactory] :: Nil

  def transformActivities: List[Class[_ <: AnyPlugin]] =
    classOf[ExecuteTransformFactory] ::
    classOf[TransformPathsCacheFactory] ::
    classOf[VocabularyCacheFactory] ::
    Nil

  def linkingActivities: List[Class[_ <: AnyPlugin]] =
    classOf[EvaluateLinkingFactory] ::
    classOf[ExecuteLinkingFactory] ::
    classOf[LinkingPathsCacheFactory] ::
    classOf[ReferenceEntitiesCacheFactory] :: Nil

  def workflowActivities: List[Class[_ <: AnyPlugin]] =
    classOf[LocalWorkflowExecutorFactory] :: Nil

  def formats: List[Class[_ <: AnyPlugin]] = {
    TypesFormat.getClass ::
    VocabularyCacheValue.ValueFormat.getClass ::
    CachedEntitySchemaXmlFormat.getClass ::
    WorkflowOperatorsFormat.getClass ::
    WorkflowDatasetsFormat.getClass ::
    TaskIdentifierParameterXmlFormat.getClass ::
    WorkflowXmlFormat.getClass ::
    Nil
  }

  def rulePlugins: List[Class[_ <: AnyPlugin]] = {
    classOf[ReadParameter] ::
    classOf[RemoveStopwords] ::
    Nil
  }

  def projectMarshaller: List[Class[_ <: AnyPlugin]] = {
    classOf[XmlZipWithResourcesProjectMarshaling] ::
    classOf[XmlZipWithoutResourcesProjectMarshaling] :: Nil
  }

  def provenancePlugins: List[Class[_ <: AnyPlugin]] = classOf[NopPersistWorkflowProvenance] :: Nil

  def autoCompletionProviderPlugins: List[Class[_ <: AnyPlugin]] = classOf[DatasetTypeAutoCompletionProvider] :: Nil

  def workspaceActivityPlugins: List[Class[_ <: AnyPlugin]] = {
    classOf[GlobalVocabularyCacheFactory] ::
      classOf[GlobalUriPatternCacheFactory] ::
      Nil
  }
}
