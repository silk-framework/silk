package org.silkframework.workspace

import org.silkframework.runtime.plugin.PluginModule
import org.silkframework.workspace.activity.dataset.TypesCacheFactory
import org.silkframework.workspace.activity.linking._
import org.silkframework.workspace.activity.transform.{ExecuteTransformFactory, TransformPathsCacheFactory}
import org.silkframework.workspace.activity.workflow.WorkflowExecutorFactory
import org.silkframework.workspace.xml.FileWorkspaceProvider

class WorkspacePlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_]] =
    workspaceProviders :::
    datasetActivities :::
    transformActivities :::
    linkingActivities :::
    workflowActivities

  def workspaceProviders: List[Class[_]] =
    classOf[FileWorkspaceProvider] ::
    classOf[InMemoryWorkspaceProvider] :: Nil

  def datasetActivities: List[Class[_]] =
    classOf[TypesCacheFactory] :: Nil

  def transformActivities: List[Class[_]] =
    classOf[ExecuteTransformFactory] ::
    classOf[TransformPathsCacheFactory] :: Nil

  def linkingActivities: List[Class[_]] =
    classOf[GenerateLinksFactory] ::
    classOf[LearningFactory] ::
    classOf[ActiveLearningFactory] ::
    classOf[LinkingPathsCacheFactory] ::
    classOf[ReferenceEntitiesCacheFactory] :: Nil

  def workflowActivities: List[Class[_]] =
    classOf[WorkflowExecutorFactory] :: Nil
}
