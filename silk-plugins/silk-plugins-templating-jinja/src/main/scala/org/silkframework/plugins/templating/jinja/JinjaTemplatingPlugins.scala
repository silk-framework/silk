package org.silkframework.plugins.templating.jinja

import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}

class JinjaTemplatingPlugins extends PluginModule {
  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] = Seq(classOf[JinjaTemplateEngine])
}
