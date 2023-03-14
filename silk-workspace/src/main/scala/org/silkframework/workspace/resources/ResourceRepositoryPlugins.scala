package org.silkframework.workspace.resources

import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}

class ResourceRepositoryPlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] =
    Seq(classOf[PerProjectFileRepository], classOf[SharedFileRepository], classOf[InMemoryResourceRepository], classOf[EmptyResourceRepository])
}
