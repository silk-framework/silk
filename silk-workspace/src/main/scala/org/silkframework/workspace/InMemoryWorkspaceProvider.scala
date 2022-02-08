package org.silkframework.workspace

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.config.{Tag, Task, TaskSpec}
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.{InMemoryResourceManager, ResourceManager}
import org.silkframework.util.Identifier
import org.silkframework.workspace.io.WorkspaceIO

import scala.reflect.ClassTag

@Plugin(
  id = "inMemory",
  label = "In-memory workspace",
  description = "Workspace provider that holds all projects in memory. All contents will be gone on restart."
)
class InMemoryWorkspaceProvider() extends WorkspaceProvider {

  protected var projects = Map[Identifier, InMemoryProject]()

  /**
    * Reads all projects from the workspace.
    */
  override def readProjects()
                           (implicit userContext: UserContext): Seq[ProjectConfig] = projects.values.map(_.config).toSeq

  /**
    * Adds/Updates a project.
    */
  override def putProject(project: ProjectConfig)
                         (implicit userContext: UserContext): Unit = {
    projects.get(project.id) match {
      case Some(existingProject) =>
        existingProject.config = project
      case None =>
        projects += ((project.id, new InMemoryProject(project.copy(projectResourceUriOpt = Some(project.resourceUriOrElseDefaultUri)))))
    }
  }

  /**
    * Deletes a project.
    */
  override def deleteProject(name: Identifier)
                            (implicit userContext: UserContext): Unit = {
    projects -= name
  }

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
  override def projectCache(name: Identifier): ResourceManager = projects(name).cache

  /**
    * Adds/Updates a task in a project.
    */
  override def putTask[T <: TaskSpec : ClassTag](project: Identifier, task: Task[T])
                                                (implicit userContext: UserContext): Unit = {
    projects(project).tasks += ((task.id, LoadedTask.success(task)))
  }

  /**
    * Reads all tasks of a specific type from a project.
    */
  override def readTasks[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)
                                                  (implicit user: UserContext): Seq[LoadedTask[T]] = {
    val requestedClass = implicitly[ClassTag[T]].runtimeClass
    projects(project).tasks.values.filter(task => requestedClass.isAssignableFrom(task.taskType)).map(_.asInstanceOf[LoadedTask[T]]).toSeq
  }

  /**
    * Reads all tasks of all types from a project.
    **/
  override def readAllTasks(project: Identifier, projectResources: ResourceManager)
                           (implicit user: UserContext): Seq[LoadedTask[_]] = {
    projects(project).tasks.values.toSeq
  }

  /**
    * Deletes a task from a project.
    */
  override def deleteTask[T <: TaskSpec : ClassTag](project: Identifier, task: Identifier)
                                                   (implicit userContext: UserContext): Unit = {
    projects(project).tasks -= task
  }

  /**
    * Retrieve a list of all available tags.
    */
  def readTags(project: Identifier)
              (implicit userContext: UserContext): Iterable[Tag] = {
    projects(project).tags.values
  }

  /**
    * Add a new tag.
    * Adding a tag with an existing URI, will overwrite the corresponding tag.
    */
  def putTag(project: Identifier, tag: Tag)
            (implicit userContext: UserContext): Unit = {
    projects(project).tags += ((tag.uri, tag))
  }

  /**
    * Remove a tag.
    */
  def deleteTag(project: Identifier, tagUri: String)
               (implicit userContext: UserContext): Unit = {
    projects(project).tags -= tagUri
  }

  /**
    * No refresh needed.
    */
  override def refresh()(implicit userContext: UserContext): Unit = {}

  protected class InMemoryProject(@volatile var config: ProjectConfig) {

    var tasks: Map[Identifier, LoadedTask[_ <: TaskSpec]] = Map.empty

    var tags: Map[String, Tag] = Map.empty

    val resources = new InMemoryResourceManager

    val cache = new InMemoryResourceManager

  }

  override def readProject(projectId: String)
                          (implicit userContext: UserContext): Option[ProjectConfig] = {
    projects.get(projectId).map(_.config)
  }

  /**
    * Returns None, because the projects are not held as RDF.
    */
  override def sparqlEndpoint: Option[SparqlEndpoint] = None
}
