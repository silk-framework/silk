package de.fuberlin.wiwiss.silk.workspace

import de.fuberlin.wiwiss.silk.runtime.plugin.PluginModule
import de.fuberlin.wiwiss.silk.workspace.xml.FileWorkspaceProvider

class WorkspacePlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_]] = classOf[FileWorkspaceProvider] :: classOf[DefaultActivities] :: Nil
}
