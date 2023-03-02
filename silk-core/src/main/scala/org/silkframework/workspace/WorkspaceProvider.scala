package org.silkframework.workspace

import org.silkframework.config.{Prefixes, Tag, Task, TaskSpec}
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier
import org.silkframework.workspace.resources.ResourceRepository

import scala.collection.mutable
import scala.reflect.ClassTag

trait WorkspaceProvider {

  /**
   * Reads all projects from the workspace.
   */
  def readProjects()(implicit user: UserContext): Seq[ProjectConfig]

  /**
    * Reads a single project from the backend.
    * @return The project config or None if the project does not exist or an error occurred.
    */
  def readProject(projectId: String)(implicit userContext: UserContext): Option[ProjectConfig]

  /**
   * Adds/Updates a project.
   */
  def putProject(projectConfig: ProjectConfig)(implicit user: UserContext): Unit

  /**
   * Deletes a project.
   */
  def deleteProject(name: Identifier)(implicit user: UserContext): Unit

  /**
    * Imports a complete project.
    */
  def importProject(project: ProjectConfig,
                    provider: WorkspaceProvider,
                    inputResources: Option[ResourceManager],
                    outputResources: Option[ResourceManager])(implicit user: UserContext): Unit

  /**
   * Retrieves the project cache folder.
   */
  def projectCache(name: Identifier): ResourceManager

  /**
    * Reads all tasks of a specific type from a project.
    **/
  def readTasks[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)
                                         (implicit user: UserContext): Seq[LoadedTask[T]]

  /**
    * Reads all tasks of all types from a project.
    **/
  def readAllTasks(project: Identifier, projectResources: ResourceManager)
                  (implicit user: UserContext): Seq[LoadedTask[_]]

  /**
   * Adds/Updates a task in a project.
    *
    * @param projectResourceManager The resource manager that is used to serialize the path of project resources correctly.
   */
  def putTask[T <: TaskSpec : ClassTag](project: Identifier,
                                        task: Task[T],
                                        projectResourceManager: ResourceManager)
                                       (implicit user: UserContext): Unit

  /**
   * Deletes a task from a project.
   */
  def deleteTask[T <: TaskSpec : ClassTag](project: Identifier, task: Identifier)(implicit user: UserContext): Unit

  /**
    * Retrieve a list of all available tags.
    */
  def readTags(project: Identifier)
              (implicit userContext: UserContext): Iterable[Tag]

  /**
    * Add a new tag.
    * Adding a tag with an existing URI, will overwrite the corresponding tag.
    */
  def putTag(project: Identifier, tag: Tag)
            (implicit userContext: UserContext): Unit

  /**
    * Adds a set of new tags.
    */
  def putTags(project: Identifier, tags: Iterable[Tag])
             (implicit userContext: UserContext): Unit = {
    for(tag <- tags) {
      putTag(project, tag)
    }
  }

  /**
    * Remove a tag.
    */
  def deleteTag(project: Identifier, tagUri: String)
               (implicit userContext: UserContext): Unit

  /**
    * Refreshes all projects, i.e. cleans all possible caches if there are any and reloads all projects freshly.
    */
  def refresh(projectResources: ResourceRepository)(implicit userContext: UserContext): Unit

  /** Fetches registered prefix definitions, e.g. from known voabularies. */
  def fetchRegisteredPrefixes()(implicit userContext: UserContext): Prefixes = {
    // Most workspace providers won't be able to offer this functionality, since they are not vocabulary aware, so this defaults to empty prefixes.
    Prefixes.empty
  }

  /**
    * Returns an SPARQL endpoint that allows query access to the projects.
    * May return None if the projects are not held as RDF.
    */
  def sparqlEndpoint: Option[SparqlEndpoint]

  private val externalLoadingErrors: mutable.HashMap[String, Vector[TaskLoadingError]] = new mutable.HashMap[String, Vector[TaskLoadingError]]()

  /**
    * Retains a task loading error that was caught from an external system, e.g. when copying tasks between workspaces (project import),
    * the loading error is also copied and can be displayed to the user.
    */
  def retainExternalTaskLoadingError(projectId: String,
                                     loadingError: TaskLoadingError): Unit = synchronized {
    val loadingErrors = externalLoadingErrors.getOrElse(projectId, Vector.empty)
    val loadingErrorsWithoutTaskId = loadingErrors.filter(_.taskId != loadingError.taskId)
    externalLoadingErrors.put(projectId, loadingErrorsWithoutTaskId :+ loadingError)
  }

  /** Task loading errors that come from an external system, e.g. when copying tasks between workspaces. The errors should be
    * kept e.g. for informing the user. */
  private[workspace] def externalTaskLoadingErrors(projectId: String): Seq[TaskLoadingError] = synchronized {
    externalLoadingErrors.get(projectId).toSeq.flatten
  }

  /** Removes the external task loading errors that were previously added. */
  private[workspace] def removeExternalTaskLoadingErrors(projectId: String): Unit = synchronized {
    externalLoadingErrors.remove(projectId)
  }

  /** Removes the external task loading error that might exist. */
  private[workspace] def removeExternalTaskLoadingError(projectId: String, taskId: String): Unit = synchronized {
    val loadingErrors = externalLoadingErrors.getOrElse(projectId, Vector.empty)
    externalLoadingErrors.put(projectId, loadingErrors.filter(_.taskId != taskId))
  }
}


/**
  * The result of loading a task.
  * Holds either the loaded task or the loading error.
  */
case class LoadedTask[T <: TaskSpec : ClassTag](taskOrError: Either[TaskLoadingError, Task[T]]) {

  /**
    * The task type.
    */
  val taskType: Class[_] = implicitly[ClassTag[T]].runtimeClass

  /**
    * Retrieves the task if it could be loaded or throws an exception containing the tasks loading error.
    */
  def task: Task[T] = {
    taskOrError.right.getOrElse(throw taskOrError.left.get.throwable)
  }

  /**
    * Returns the task if it could be loaded or None otherwise.
    */
  def taskOption: Option[Task[T]] = {
    taskOrError.toOption
  }

  /**
    * Retrieves the loading error, if any.
    */
  def error: Option[TaskLoadingError] = taskOrError.swap.toOption

}

object LoadedTask {

  def success[T <: TaskSpec : ClassTag](task: Task[T]): LoadedTask[T] = {
    LoadedTask(Right(task))
  }

  def failed[T <: TaskSpec : ClassTag](error: TaskLoadingError): LoadedTask[T] = {
    LoadedTask(Left(error))
  }

}

/** Task loading error. */
case class TaskLoadingError(projectId: Option[String], taskId: String, throwable: Throwable, label: Option[String] = None, description: Option[String] = None)
