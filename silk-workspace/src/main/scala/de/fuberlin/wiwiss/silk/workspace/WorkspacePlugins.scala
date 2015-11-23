package de.fuberlin.wiwiss.silk.workspace

import de.fuberlin.wiwiss.silk.runtime.plugin.PluginModule
import de.fuberlin.wiwiss.silk.workspace.xml.FileWorkspaceProvider

class WorkspacePlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_]] =
    classOf[FileWorkspaceProvider] ::
    datasetActivities :::
    transformActivities :::
    linkingActivities :::
    workflowActivities

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
