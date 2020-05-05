package org.silkframework.workspace

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier
import org.silkframework.workspace.io.WorkspaceIO
import org.silkframework.workspace.resources.ResourceRepository

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
    * Imports a complete project.
    */
  def importProject(project: ProjectConfig,
                    provider: WorkspaceProvider,
                    inputResources: Option[ResourceManager],
                    outputResources: Option[ResourceManager])(implicit user: UserContext): Unit = {
    WorkspaceIO.copyProject(provider, this, inputResources, outputResources, project)
  }

  /**
   * Retrieves the project cache folder.
   */
  def projectCache(name: Identifier): ResourceManager

  /**
   * Reads all tasks of a specific type from a project.
   */
  def readTasks[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)
                                         (implicit user: UserContext): Seq[Task[T]] = {
    readTasksSafe[T](project, projectResources).map(_.get)
  }

  /**
    * Version of readTasks that returns a Seq[Try[Task[T]]]
    **/
  def readTasksSafe[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)(implicit user: UserContext): Seq[Try[Task[T]]]

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
}
