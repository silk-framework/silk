package org.silkframework.workspace

import org.silkframework.runtime.users.User
import org.silkframework.workspace.exceptions.AccessDeniedException

/**
 * Manages the user groups of the current user. This is used to determine which projects a user has access to.
 *
 * Thread safe.
 */
class AccessControlManager {

  // TODO we should persist the groups in the backend as well
  @volatile
  private var groups = Set[String]()

  /**
   * Sets the user groups of the current user.
   */
  def setGroups(groups: Set[String]): Unit = {
    this.groups = groups
  }

  /**
   * Returns the user groups of the current user.
   */
  def getGroups: Set[String] = {
    groups
  }

  /**
   * Checks if the current user has access to the project.
   * Returns silently if the user has access.
   *
   * @throws AccessDeniedException If the user does not have access to the project.
   */
  def checkAccess(user: User): Unit = {
    if(!user.groups.exists(groups.contains)) {
      throw AccessDeniedException("User does not have access to this project.")

    }
  }

}
