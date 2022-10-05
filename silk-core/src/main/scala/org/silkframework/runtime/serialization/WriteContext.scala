package org.silkframework.runtime.serialization

import org.silkframework.config.Prefixes
import org.silkframework.workspace.ProjectTrait

/**
  * Holds context information when serializing data.
  */
case class WriteContext[U](parent: Option[U] = None, prefixes: Prefixes = Prefixes.empty, projectId: Option[String] = None, projectUri: Option[String] = None)

object WriteContext {

  def forProject[U](project: ProjectTrait, parent: Option[U] = None): WriteContext[U] = {
    WriteContext[U](
      parent = parent,
      prefixes = project.config.prefixes,
      projectId = Some(project.id),
      projectUri = project.config.projectResourceUriOpt
    )
  }

}
