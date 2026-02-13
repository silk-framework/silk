package org.silkframework.workspace

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

}
