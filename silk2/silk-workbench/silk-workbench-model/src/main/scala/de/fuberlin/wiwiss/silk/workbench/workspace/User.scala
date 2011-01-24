package de.fuberlin.wiwiss.silk.workbench.workspace

import java.io.File

class User
{
  //val id = Random.nextInt

  val project = new FileProject(new File("."))
}

object User
{
  //var userManager : () => User = null

  /**
   * Retrieves the current user.
   */
  def apply() = new User
}