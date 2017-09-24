package org.silkframework.workspace

import org.silkframework.runtime.plugin.PluginModule
import org.silkframework.workspace.activity.dataset.Types.TypesFormat
import org.silkframework.workspace.activity.dataset.{Types, TypesCacheFactory}
import org.silkframework.workspace.activity.linking._
import org.silkframework.workspace.activity.transform.{ExecuteTransformFactory, TransformPathsCacheFactory, VocabularyCache, VocabularyCacheFactory}
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorFactory, WorkflowExecutionReportJsonFormat}
import org.silkframework.workspace.xml.{FileWorkspaceProvider, XmlZipProjectMarshaling}

import scala.language.existentials

class WorkspacePlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_]] =
    workspaceProviders :::
    datasetActivities :::
    transformActivities :::
    linkingActivities :::
    workflowActivities :::
    projectMarshaller :::
    formats

  def workspaceProviders: List[Class[_]] =
    classOf[FileWorkspaceProvider] ::
    classOf[InMemoryWorkspaceProvider] :: Nil

  def datasetActivities: List[Class[_]] =
    classOf[TypesCacheFactory] :: Nil

  def transformActivities: List[Class[_]] =
    classOf[ExecuteTransformFactory] ::
    classOf[TransformPathsCacheFactory] ::
    classOf[VocabularyCacheFactory] :: Nil

  def linkingActivities: List[Class[_]] =
    classOf[GenerateLinksFactory] ::
    classOf[LearningFactory] ::
    classOf[ActiveLearningFactory] ::
    classOf[LinkingPathsCacheFactory] ::
    classOf[ReferenceEntitiesCacheFactory] :: Nil

  def workflowActivities: List[Class[_]] =
    classOf[LocalWorkflowExecutorFactory] ::
        Nil

  def formats = {
    TypesFormat.getClass ::
    VocabularyCache.ValueFormat.getClass ::
    classOf[WorkflowExecutionReportJsonFormat] :: Nil
  }

  def projectMarshaller = {
    classOf[XmlZipProjectMarshaling] :: Nil
  }
}
