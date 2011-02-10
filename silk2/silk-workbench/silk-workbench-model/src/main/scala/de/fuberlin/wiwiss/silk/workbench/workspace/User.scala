package de.fuberlin.wiwiss.silk.workbench.workspace

import java.io.File
import modules.linking.LinkingTask
import java.net.URI

/**
 * Dummy user as there is no user management yet.
 */
trait User
{
  private var currentLinkingTask : Option[LinkingTask] = None

  /**
   * The current workspace of this user
   */
  def workspace : Workspace

  // TODO - to be remove - used as fake project selection for the workbench-model package
  var project = workspace.projects.toSeq.last

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
  private val user = new FileUser()

  /**
   *  Retrieves the current user.
   */
  def apply() = user
}