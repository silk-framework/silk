package de.fuberlin.wiwiss.silk.workbench.workspace

import java.io.File
import modules.linking.LinkingTask

/**
 * Dummy user as there is no user management yet.
 */
class User
{
  /**
   * The current project of this user.
   */
  val project =
  {
    val projectFile = new File("./workspace/project/")
    projectFile.mkdirs()
    new FileProject(projectFile)
  }

  /**
   * The current linking tasks of this user.
   */
  var linkingTask : Option[LinkingTask] = None
}

object User
{
  /**
   *  Retrieves the current user.
   */
  def apply() = new User
}