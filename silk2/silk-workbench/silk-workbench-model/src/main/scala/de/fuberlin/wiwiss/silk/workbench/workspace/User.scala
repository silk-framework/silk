/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workbench.workspace

import modules.linking.LinkingTask
import modules.ModuleTask
import modules.output.OutputTask
import modules.source.SourceTask
import de.fuberlin.wiwiss.silk.util.Observable
import de.fuberlin.wiwiss.silk.workbench.evaluation.CurrentGenerateLinksTask
import de.fuberlin.wiwiss.silk.GenerateLinksTask
import de.fuberlin.wiwiss.silk.workbench.workspace.User.{CurrentProjectChanged, CurrentTaskChanged}

/**
 * A user.
 */
trait User extends Observable[User.Message] {

  @volatile private var currentProject: Option[Project] = None

  @volatile private var currentTask: Option[ModuleTask] = None

  /**
   * The current workspace of this user.
   */
  def workspace: Workspace

  def projectOpen = currentProject.isDefined

  /**
   * The current project of this user.
   */
  def project = currentProject.getOrElse(throw new NoSuchElementException("No active project"))

  /**
   * Sets the current project of this user.
   */
  def project_=(project: Project) {
    val previousProject = currentProject
    currentProject = Some(project)
    publish(CurrentProjectChanged(this, previousProject, project))
  }

  /**
   * True if a task if open at the moment.
   */
  def taskOpen = currentTask.isDefined

  /**
   * The current task of this user.
   */
  def task = currentTask.getOrElse(throw new NoSuchElementException("No active task"))

  /**
   * Sets the current task of this user.
   */
  def task_=(task: ModuleTask) {
    val previousTask = currentTask
    currentTask = Some(task)
    publish(CurrentTaskChanged(this, previousTask, task))
  }

  /**
   * Closes the current task.
   */
  def closeTask() {
    currentTask = None
  }

  /**
   * True, if a source task is open at the moment.
   */
  def sourceTaskOpen = taskOpen && task.isInstanceOf[SourceTask]

  /**
   * The current source task of this user.
   *
   * @throws java.util.NoSuchElementException If no source task is open
   */
  def sourceTask = task match {
    case t: SourceTask => t
    case _ => throw new NoSuchElementException("Active task is no source task")
  }

  /**
   * True, if a linking task is open at the moment.
   */
  def linkingTaskOpen = taskOpen && task.isInstanceOf[LinkingTask]

  /**
   * The current linking tasks of this user.
   *
   * @throws java.util.NoSuchElementException If no linking task is open
   */
  def linkingTask = task match {
    case t: LinkingTask => t
    case _ => throw new NoSuchElementException("Active task is no linking task")
  }

  /**
   * True, if a output task is open at the moment.
   */
  def outputTaskOpen = taskOpen && task.isInstanceOf[OutputTask]

  /**
   * The current output task of this user.
   *
   * @throws java.util.NoSuchElementException If no output task is open
   */
  def outputTask = task match {
    case t: OutputTask => t
    case _ => throw new NoSuchElementException("Active task is no output task")
  }

  /**
   * Called when the user becomes inactive.
   */
  def dispose() {
    CurrentGenerateLinksTask().cancel()
  }
}

object User {
  var userManager: () => User = () => throw new Exception("No user manager registered")

  /**
   * Retrieves the current user.
   */
  def apply() = userManager()

  sealed trait Message

  /**
   * Fired if the current project is changed.
   */
  case class CurrentProjectChanged(user: User, previousProject: Option[Project], project: Project) extends Message

  /**
   * Fired if the current task is changed.
   */
  case class CurrentTaskChanged(user: User, previousTask: Option[ModuleTask], task: ModuleTask) extends Message
}