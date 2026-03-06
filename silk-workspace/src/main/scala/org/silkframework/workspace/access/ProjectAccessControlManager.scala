package org.silkframework.workspace.access

import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier
import org.silkframework.workspace.{AccessControl, WorkspaceProvider}

/**
 * Manages the user groups of the current user. This is used to determine which projects a user has access to.
 *
 * Thread safe.
 */
class ProjectAccessControlManager(project: Identifier, provider: WorkspaceProvider, loadingUser: UserContext) {

  @volatile
  private var accessControl = AccessControl.empty

  private var loaded: Boolean = false

  /**
   * Sets the access control groups for the project.
   */
  def setGroups(groups: Set[String]): Unit = synchronized {
    implicit val userContext: UserContext = loadingUser
    loadIfRequired()
    val newAccessControl = AccessControl(groups)
    provider.putAccessControl(project, newAccessControl)
    this.accessControl = newAccessControl
  }

  /**
   * Returns the access control groups for the project.
   */
  def getGroups: Set[String] = synchronized {
    implicit val userContext: UserContext = loadingUser
    loadIfRequired()
    accessControl.groups
  }

  /**
   * Checks if the current user has access to the project.
   * Returns silently if the user has access.
   *
   * @throws ProjectAccessDeniedException If the user does not have access to the project.
   */
  def checkAccess()(implicit userContext: UserContext): Unit = synchronized {
    if(!hasAccess()) {
      userContext.user match {
        case Some(user) =>
          throw ProjectAccessDeniedException(
            s"User does not have access to this project. Required groups: ${accessControl.groups.mkString(", ")}. User groups: ${user.groups.mkString(", ")}")
        case None =>
          throw ProjectAccessDeniedException("No user supplied.")
      }

    }
  }

  /**
   * Checks if the current user has access to the project.
   *
   * @return True if the user has access to the project, false otherwise.
   */
  def hasAccess()(implicit userContext: UserContext): Boolean = synchronized {
    val config = AccessControlConfig()
    if(config.enabled) {
      loadIfRequired()
      userContext.user match {
        case Some(user) =>
          val requiredGroups = accessControl.groups
          user.actions.contains(config.adminAction) || requiredGroups.isEmpty || user.groups.exists(requiredGroups.contains)
        case None =>
          false
      }
    } else {
      true
    }
  }

  private def loadIfRequired()(implicit userContext: UserContext): Unit = {
    if(!loaded) {
      accessControl = provider.readAccessControl(project).getOrElse(AccessControl.empty)
      loaded = true
    }
  }

}
