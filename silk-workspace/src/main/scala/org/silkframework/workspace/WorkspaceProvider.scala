package org.silkframework.workspace

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier

import scala.reflect.ClassTag
import scala.util.Try

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
   * Retrieves the project cache folder.
   */
  def projectCache(name: Identifier): ResourceManager

  /**
   * Reads all tasks of a specific type from a project.
    *
    * Use readTasksSafe instead of this method.
   */
  def readTasks[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)
                                         (implicit user: UserContext): Seq[Task[T]] = {
    readTasksSafe[T](project, projectResources).map(t => t.left.getOrElse(throw t.right.get.throwable))
  }

  /**
    * Version of readTasks that returns a Seq[Try[Task[T]]]
    **/
  def readTasksSafe[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)(implicit user: UserContext): Seq[Either[Task[T], TaskLoadingError]]

  /**
   * Adds/Updates a task in a project.
   */
  def putTask[T <: TaskSpec : ClassTag](project: Identifier, task: Task[T])(implicit user: UserContext): Unit

  /**
   * Deletes a task from a project.
   */
  def deleteTask[T <: TaskSpec : ClassTag](project: Identifier, task: Identifier)(implicit user: UserContext): Unit

  /**
    * Refreshes all projects, i.e. cleans all possible caches if there are any and reloads all projects freshly.
    */
  def refresh()(implicit userContext: UserContext): Unit

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

  private var externalLoadingErrors: List[TaskLoadingError] = List.empty

  /**
    * Retains a task loading error that was caught from an external system, e.g. when copying tasks between workspaces (project import),
    * the loading error is also copied and can be displayed to the user.
    */
  private[workspace] def retainExternalTaskLoadingError(loadingError: TaskLoadingError): Unit = synchronized {
    externalLoadingErrors = externalLoadingErrors.filter(_.id != loadingError.id)
    externalLoadingErrors ::= loadingError
  }

  /** Task loading errors that come from an external system, e.g. when copying tasks between workspaces. The errors should be
    * kept e.g. for informing the user. */
  private[workspace] def externalTaskLoadingErrors: Seq[TaskLoadingError] = synchronized {
    externalLoadingErrors
  }

}

/** Task loading error. */
case class TaskLoadingError(id: String, throwable: Throwable, label: Option[String] = None, description: Option[String] = None)