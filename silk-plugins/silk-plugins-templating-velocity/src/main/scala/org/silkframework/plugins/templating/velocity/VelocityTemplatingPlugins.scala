package org.silkframework.plugins.templating.velocity

import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}

class VelocityTemplatingPlugins extends PluginModule {
  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] = Seq(classOf[VelocityTemplateEngine])
}
