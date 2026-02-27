package org.silkframework.workspace

import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier

/**
  * Defines the read-only API of the workspace.
  */
trait WorkspaceReadTrait {

  /**
   * The projects of the workspace the user has access to.
   */
  def userProjects(implicit userContext: UserContext): Seq[ProjectTrait]

  /**
   * Retrieves a project by name.
   */
  def project(name: Identifier)(implicit userContext: UserContext): ProjectTrait

  /**
   * Retrieves a project by name. If no project with the given name has been found, then None is returned.
   */
  def projectOption(name: Identifier)(implicit userContext: UserContext): Option[ProjectTrait]
}
