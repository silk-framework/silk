package org.silkframework.workbench.rules

import org.silkframework.runtime.plugin.PluginModule

class WorkbenchRulePlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_]] = Seq(classOf[LinkingPlugin], classOf[TransformPlugin])
}
