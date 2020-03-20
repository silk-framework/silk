package org.silkframework.workspace

import org.silkframework.config.{MetaData, PlainTask, Task, TaskSpec}
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.{EmptyResourceManager, InMemoryResourceManager, ResourceLoader, ResourceManager}
import org.silkframework.util.Identifier

import scala.reflect.ClassTag
import scala.util.{Success, Try}

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
    projects += ((project.id, new InMemoryProject(project.copy(projectResourceUriOpt = Some(project.resourceUriOrElseDefaultUri)))))
  }

  /**
    * Deletes a project.
    */
  override def deleteProject(name: Identifier)
                            (implicit userContext: UserContext): Unit = {
    projects -= name
  }

  /**
    * Retrieves the project cache folder.
    */
  override def projectCache(name: Identifier): ResourceManager = projects(name).cache

  /**
    * Adds/Updates a task in a project.
    */
  override def putTask[T <: TaskSpec : ClassTag](project: Identifier, task: Task[T], projectResources: ResourceLoader)
                                                (implicit userContext: UserContext): Unit = {
    projects(project).tasks += ((task.id, task))
  }

  /**
    * Reads all tasks of a specific type from a project.
    */
  override def readTasks[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)
                                                  (implicit userContext: UserContext): Seq[Task[T]] = {
    val taskClass = implicitly[ClassTag[T]].runtimeClass
    projects(project).tasks.values.filter(task => taskClass.isAssignableFrom(task.data.getClass)).map(_.asInstanceOf[Task[T]]).toSeq
  }

  override def readTasksSafe[T <: TaskSpec : ClassTag](project: Identifier,
                                                       projectResources: ResourceManager)(implicit user: UserContext): Seq[Try[Task[T]]] = {
    val taskClass = implicitly[ClassTag[T]].runtimeClass
    projects(project).tasks.values.filter(task => taskClass.isAssignableFrom(task.data.getClass)).map(task => Success(task.asInstanceOf[Task[T]])).toSeq
  }

  /**
    * Deletes a task from a project.
    */
  override def deleteTask[T <: TaskSpec : ClassTag](project: Identifier, task: Identifier)
                                                   (implicit userContext: UserContext): Unit = {
    projects(project).tasks -= task
  }

  /**
    * No refresh needed.
    */
  override def refresh()(implicit userContext: UserContext): Unit = {}

  protected class InMemoryProject(val config: ProjectConfig) {

    var tasks: Map[Identifier, Task[_]] = Map.empty

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
