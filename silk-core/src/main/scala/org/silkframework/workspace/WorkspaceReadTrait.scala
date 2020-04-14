package org.silkframework.workspace

import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier

/**
  * Defines the read-only API of the workspace.
  */
trait WorkspaceReadTrait {
  /** The projects of the workspace the user has access to.  */
  def projects(implicit userContext: UserContext): Seq[ProjectTrait]

  /** A specific project. */
  def project(name: Identifier)(implicit userContext: UserContext): ProjectTrait

  /**  */
  def findProject(name: Identifier)(implicit userContext: UserContext): Option[ProjectTrait]
}
