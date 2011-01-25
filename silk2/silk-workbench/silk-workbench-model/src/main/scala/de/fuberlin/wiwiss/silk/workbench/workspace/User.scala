package de.fuberlin.wiwiss.silk.workbench.workspace

import java.io.File

/**
 * Dummy user as there is no user management yet.
 */
class User
{
  val project =
  {
    val projectFile = new File("./workspace/project/")
    projectFile.mkdirs()
    new FileProject(projectFile)
  }
}

object User
{
  /**
   * Retrieves the current user.
   */
  def apply() = new User
}