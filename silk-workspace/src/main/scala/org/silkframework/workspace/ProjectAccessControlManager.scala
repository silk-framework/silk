package org.silkframework.workspace

import com.typesafe.config.Config
import org.silkframework.config.{AccessControl, ConfigValue}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.users.User
import org.silkframework.util.Identifier
import org.silkframework.workspace.exceptions.ProjectAccessDeniedException

object ProjectAccessControlManager {
  val enabled: ConfigValue[Boolean] = (config: Config) => {
    config.getBoolean("workspace.accessControl.enabled")
  }
}

/**
 * Manages the user groups of the current user. This is used to determine which projects a user has access to.
 *
 * Thread safe.
 */
class ProjectAccessControlManager(project: Identifier, provider: WorkspaceProvider) {

  @volatile
  private var accessControl = AccessControl.empty

  private var loaded: Boolean = false

  /**
   * Sets the access control groups for the project.
   */
  def setGroups(groups: Set[String])(implicit userContext: UserContext): Unit = synchronized {
    loadIfRequired()
    val newAccessControl = AccessControl(groups)
    provider.putAccessControlGroups(project, newAccessControl)
    this.accessControl = newAccessControl
  }

  /**
   * Returns the access control groups for the project.
   */
  def getGroups(implicit userContext: UserContext): Set[String] = synchronized {
    loadIfRequired()
    accessControl.groups
  }

  /**
   * Checks if the current user has access to the project.
   * Returns silently if the user has access.
   *
   * @throws ProjectAccessDeniedException If the user does not have access to the project.
   */
  def checkAccess(user: User)(implicit userContext: UserContext): Unit = synchronized {
    if(!hasAccess(user)) {
      throw ProjectAccessDeniedException(s"User does not have access to this project. Required groups: ${user.groups.mkString(", ")}. User groups: ${user.groups.mkString(", ")}")
    }
  }

  /**
   * Checks if the current user has access to the project.
   *
   * @return True if the user has access to the project, false otherwise.
   */
  def hasAccess(user: User)(implicit userContext: UserContext): Boolean = synchronized {
    if(ProjectAccessControlManager.enabled()) {
      loadIfRequired()
      val groups = accessControl.groups
      groups.isEmpty || user.groups.exists(groups.contains)
    } else {
      true
    }
  }

  private def loadIfRequired()(implicit userContext: UserContext): Unit = {
    if(!loaded) {
      accessControl = provider.readAccessControlGroups(project)
      loaded = true
    }
  }

}
