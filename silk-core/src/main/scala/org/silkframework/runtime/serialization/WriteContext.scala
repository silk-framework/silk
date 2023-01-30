package org.silkframework.runtime.serialization

import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.util.Identifier
import org.silkframework.workspace.ProjectTrait

/**
  * Holds context information when serializing data.
  */
case class WriteContext[U](parent: Option[U] = None,
                           prefixes: Prefixes = Prefixes.empty,
                           projectId: Option[Identifier] = None,
                           projectUri: Option[String] = None,
                           resources: ResourceManager,
                           user: UserContext = UserContext.Empty
                          ) extends PluginContext

object WriteContext {
  def empty[U]: WriteContext[U] = WriteContext(resources = EmptyResourceManager(), user = UserContext.Empty)

  def forProject[U](project: ProjectTrait,
                    parent: Option[U] = None)
                   (implicit userContext: UserContext): WriteContext[U] = {
    WriteContext[U](
      parent = parent,
      prefixes = project.config.prefixes,
      projectId = Some(project.id),
      projectUri = project.config.projectResourceUriOpt,
      resources = project.resources,
      user = userContext
    )
  }
}
