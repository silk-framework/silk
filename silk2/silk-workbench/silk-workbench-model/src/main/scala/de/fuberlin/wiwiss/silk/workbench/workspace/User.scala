package de.fuberlin.wiwiss.silk.workbench.workspace

import java.io.File

/**
 * Dummy user as there is no user management yet.
 */
class User
{
  val project = new FileProject(new File("."))
}

object User
{
  /**
   * Retrieves the current user.
   */
  def apply() = new User
}