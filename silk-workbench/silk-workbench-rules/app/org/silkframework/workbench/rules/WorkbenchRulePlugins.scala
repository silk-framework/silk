package org.silkframework.workbench.rules

import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}

class WorkbenchRulePlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] = Seq(classOf[LinkingPlugin], classOf[TransformPlugin])
}
