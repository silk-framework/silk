package de.fuberlin.wiwiss.silk.workbench.workspace

import java.io.File
import modules.linking.LinkingTask

/**
 * Dummy user as there is no user management yet.
 */
class User
{
  private var currentLinkingTask : Option[LinkingTask] = None

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
   * True, if a linking task is open at the moment.
   */
  def linkingTaskOpen = currentLinkingTask.isDefined

  /**
   * The current linking tasks of this user.
   */
  //TODO document exception
  def linkingTask = currentLinkingTask.getOrElse(throw new IllegalStateException("No active linking task"))

  def linkingTask_=(task : LinkingTask) =
  {
    currentLinkingTask = Some(task)
  }
}

object User
{
  private val user = new User()

  /**
   *  Retrieves the current user.
   */
  def apply() = user
}