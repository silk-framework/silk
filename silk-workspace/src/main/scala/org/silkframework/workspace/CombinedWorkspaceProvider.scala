package org.silkframework.workspace

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.runtime.resource.{FileResourceManager, ResourceManager, UrlResourceManager}
import org.silkframework.util.Identifier
import org.silkframework.workspace.io.WorkspaceIO
import org.silkframework.workspace.xml.XmlWorkspaceProvider

import scala.reflect.ClassTag
import scala.util.Try

class CombinedWorkspaceProvider(primaryWorkspace: WorkspaceProvider,
                                secondaryWorkspace: WorkspaceProvider) extends WorkspaceProvider {

  override def sparqlEndpoint: Option[SparqlEndpoint] = primaryWorkspace.sparqlEndpoint.orElse(secondaryWorkspace.sparqlEndpoint)

  /**
    * Refreshes all projects, i.e. cleans all possible caches if there are any and reloads all projects freshly.
    */
  override def refresh()(implicit userContext: UserContext): Unit = {
    primaryWorkspace.refresh()

    // Delete all projects from the secondary workspace provider and recommit them
    // We do this to avoid that both workspace providers get out of sync
    for(project <- secondaryWorkspace.readProjects()) {
      secondaryWorkspace.deleteProject(project.id)
    }
    WorkspaceIO.copyProjects(primaryWorkspace, secondaryWorkspace, None, None)
  }

  /**
    * Reads all projects from the workspace.
    */
  override def readProjects()(implicit user: UserContext): Seq[ProjectConfig] = {
    primaryWorkspace.readProjects()
  }

  /**
    * Reads a single project from the backend.
    *
    * @return The project config or None if the project does not exist or an error occurred.
    */
  override def readProject(projectId: String)(implicit userContext: UserContext): Option[ProjectConfig] = {
    val projectConfig = primaryWorkspace.readProject(projectId)
    for(config <- projectConfig) {
      secondaryWorkspace.deleteProject(projectId)
      secondaryWorkspace.putProject(config)
    }
    projectConfig
  }

  /**
    * Adds/Updates a project.
    */
  override def putProject(projectConfig: ProjectConfig)(implicit user: UserContext): Unit = {
    primaryWorkspace.putProject(projectConfig)
    secondaryWorkspace.putProject(projectConfig)
  }

  /**
    * Deletes a project.
    */
  override def deleteProject(name: Identifier)(implicit user: UserContext): Unit = {
    primaryWorkspace.deleteProject(name)
    secondaryWorkspace.deleteProject(name)
  }

  /**
    * Retrieves the project cache folder.
    */
  override def projectCache(name: Identifier): ResourceManager = {
    primaryWorkspace.projectCache(name)
  }

  /**
    * Version of readTasks that returns a Seq[Try[Task[T]]]
    **/
  override def readTasksSafe[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)(implicit user: UserContext): Seq[Try[Task[T]]] = {
    primaryWorkspace.readTasksSafe[T](project, projectResources)
  }

  /**
    * Adds/Updates a task in a project.
    */
  override def putTask[T <: TaskSpec : ClassTag](project: Identifier, task: Task[T])(implicit user: UserContext): Unit = {
    primaryWorkspace.putTask(project, task)
    secondaryWorkspace.putTask(project, task)
  }

  /**
    * Deletes a task from a project.
    */
  override def deleteTask[T <: TaskSpec : ClassTag](project: Identifier, task: Identifier)(implicit user: UserContext): Unit = {
    primaryWorkspace.deleteTask(project, task)
    secondaryWorkspace.deleteTask(project, task)
  }
}