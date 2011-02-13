package de.fuberlin.wiwiss.silk.workbench.workspace

import java.io.File
import modules.linking.LinkingTask
import java.net.URI
import modules.source.SourceTask

/**
 * Dummy user as there is no user management yet.
 */
trait User
{
  private var currentProject : Option[Project] = None

  private var currentSourceTask : Option[SourceTask] = None

  private var currentLinkingTask : Option[LinkingTask] = None

  /**
   * The current workspace of this user.
   */
  def workspace : Workspace

  def projectOpen = currentProject.isDefined

  /**
   * The current project of this user.
   */
  def project = currentProject.getOrElse(throw new NoSuchElementException("No active project"))

  /**
   * Sets the current project of this user.
   */
  def project_=(project : Project) =
  {
    currentProject = Some(project)
  }

  /**
   * True, if a source task is open at the moment.
   */
  def sourceTaskOpen = currentSourceTask.isDefined

  /**
   * The current source task of this user.
   *
   * @throws java.util.NoSuchElementException If no source task is open
   */
  def sourceTask = currentSourceTask.getOrElse(throw new NoSuchElementException("No active source task"))

  /**
   * Sets the current source task of this user.
   */
  def sourceTask_=(task : SourceTask) =
  {
    currentSourceTask = Some(task)
  }

  /**
   * True, if a linking task is open at the moment.
   */
  def linkingTaskOpen = currentLinkingTask.isDefined

  /**
   *  The current linking tasks of this user.
   *
   * @throws java.util.NoSuchElementException If no linking task is open
   */
  def linkingTask = currentLinkingTask.getOrElse(throw new NoSuchElementException("No active linking task"))

  /**
   * Sets the current linking task of this user.
   */
  def linkingTask_=(task : LinkingTask) =
  {
    currentLinkingTask = Some(task)
  }
}

object User
{
  var userManager : () => User = () => throw new Exception("No user manager registerd")

  /**
   * Retrieves the current user.
   */
  def apply() =  userManager()
}