package controllers.util

import controllers.workspace.MockableWorkspaceProvider
import org.silkframework.runtime.plugin.PluginModule

/**
  *
  */
class SerializationTestPlugins extends PluginModule {
  override def pluginClasses: Seq[Class[_]] = classOf[TestTraitFormatter] ::
      classOf[TestSubClassFormatter] ::
      classOf[MockableWorkspaceProvider] ::
      Nil
}
