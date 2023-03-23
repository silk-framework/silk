package controllers.util

import controllers.workspace.MockableWorkspaceProvider
import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}

/**
  *
  */
class SerializationTestPlugins extends PluginModule {
  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] = classOf[TestTraitFormatter] ::
      classOf[TestSubClassFormatter] ::
      classOf[MockableWorkspaceProvider] ::
      Nil
}
