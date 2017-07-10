package controllers.util

import org.silkframework.runtime.plugin.PluginModule

/**
  *
  */
class SerializationTestPlugins extends PluginModule {
  override def pluginClasses: Seq[Class[_]] = classOf[TestTraitFormatter] :: classOf[TestSubClassFormatter] :: Nil
}
