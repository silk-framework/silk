package de.fuberlin.wiwiss.silk.workbench.workspace

import util.Random

class User
{
  val id = Random.nextInt
}

object User
{
  var userManager : () => User = null

  /**
   * Retrieves the current user
   */
  def apply() = userManager()
}