package de.fuberlin.wiwiss.silk.workspace

import de.fuberlin.wiwiss.silk.runtime.plugin.PluginModule

class WorkspacePlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_]] = classOf[FileWorkspaceProvider] :: Nil
}
