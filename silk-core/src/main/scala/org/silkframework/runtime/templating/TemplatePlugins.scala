package org.silkframework.runtime.templating

import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}

class TemplatePlugins extends PluginModule {
  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] = Seq(classOf[DisabledTemplateEngine], classOf[UnresolvedTemplateEngine])
}
