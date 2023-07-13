package org.silkframework.workspace

import org.silkframework.config._
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.plugin.{AnyPlugin, ParameterValues, PluginContext, PluginDescription}
import org.silkframework.runtime.resource.{InMemoryResourceManager, ResourceManager}
import org.silkframework.runtime.templating.TemplateVariables
import org.silkframework.util.{Identifier, Uri}
import org.silkframework.workspace.io.WorkspaceIO
import org.silkframework.workspace.resources.ResourceRepository

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
  override def importProject(project: ProjectConfig,
                             importProvider: WorkspaceProvider,
                             importResources: ResourceManager,
                             targetResources: ResourceManager,
                             alsoCopyResources: Boolean)(implicit user: UserContext): Unit = {
    WorkspaceIO.copyProject(importProvider, this, importResources, targetResources, project, alsoCopyResources)
  }

  /**
    * Retrieves the project cache folder.
    */
  override def projectCache(name: Identifier): ResourceManager = projects(name).cache

  /**
    * Access to project variables.
    */
  def projectVariables(projectName: Identifier)
                      (implicit userContext: UserContext): TemplateVariablesSerializer = {
    projects(projectName).variables
  }

  /**
    * Adds/Updates a task in a project.
    */
  override def putTask[T <: TaskSpec : ClassTag](project: Identifier, task: Task[T], resources: ResourceManager)
                                                (implicit userContext: UserContext): Unit = {
    implicit val pluginContext: PluginContext = PluginContext(prefixes = Prefixes.empty, resources = resources, user = userContext)
    val taskType = implicitly[ClassTag[T]].runtimeClass
    val inMemoryTask =
      task.data match {
        case plugin: AnyPlugin =>
          InMemoryPluginTask(task.id, taskType, plugin.pluginSpec, plugin.parameters, task.metaData)
        case dataset: GenericDatasetSpec =>
          InMemoryDataset(task.id, taskType, dataset.plugin.pluginSpec, dataset.plugin.parameters, task.metaData, dataset.uriAttribute, dataset.readOnly)
        case _ =>
          throw new IllegalArgumentException("Non-plugin tasks are not supported: " + task)
    }
    projects(project).tasks += ((task.id, inMemoryTask))
  }

  /**
    * Reads all tasks of a specific type from a project.
    */
  override def readTasks[T <: TaskSpec : ClassTag](project: Identifier)
                                                  (implicit context: PluginContext): Seq[LoadedTask[T]] = {
    val requestedClass = implicitly[ClassTag[T]].runtimeClass

    for(task <- projects(project).tasks.values.toSeq if requestedClass.isAssignableFrom(task.taskType)) yield {
      task.load(project).asInstanceOf[LoadedTask[T]]
    }
  }

  /**
    * Reads all tasks of all types from a project.
    **/
  override def readAllTasks(project: Identifier)
                           (implicit context: PluginContext): Seq[LoadedTask[_]] = {
    for (task <- projects(project).tasks.values.toSeq) yield {
      task.load(project)
    }
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
  override def refresh(resources: ResourceRepository)(implicit userContext: UserContext): Unit = {}

  protected class InMemoryProject(@volatile var config: ProjectConfig) {

    var tasks: Map[Identifier, InMemoryTask[_ <: TaskSpec]] = Map.empty

    var tags: Map[String, Tag] = Map.empty

    val resources = new InMemoryResourceManager

    val cache = new InMemoryResourceManager

    val variables = new InMemoryTemplateVariablesSerializer

  }

  abstract class InMemoryTask[T <: TaskSpec : ClassTag] {
    def taskType: Class[_]
    def load(projectId: Identifier)(implicit pluginContext: PluginContext): LoadedTask[T]
  }

  protected case class InMemoryPluginTask[T <: TaskSpec : ClassTag](id: Identifier,
                                                                    taskType: Class[_],
                                                                    pluginDesc: PluginDescription[_],
                                                                    parameters: ParameterValues,
                                                                    metaData: MetaData) extends InMemoryTask[T] {

    def load(projectId: Identifier)(implicit pluginContext: PluginContext): LoadedTask[T] = {
      def loadInternal(parameterValues: ParameterValues, pluginContext: PluginContext): Task[T] = {
        val mergedParameters = parameters.merge(parameterValues)
        TaskLoadingException.withTaskLoadingException(OriginalTaskData(pluginDesc.id, mergedParameters)) { params =>
          LoadedTask.success[T](PlainTask(id, pluginDesc(params)(pluginContext).asInstanceOf[T], metaData)).task
        }
      }
      LoadedTask.factory[T](loadInternal, parameters, pluginContext, Some(projectId), id, metaData.label, metaData.description)
    }
  }

  protected case class InMemoryDataset[T <: TaskSpec : ClassTag](id: Identifier,
                                                                 taskType: Class[_],
                                                                 pluginDesc: PluginDescription[_],
                                                                 parameters: ParameterValues,
                                                                 metaData: MetaData,
                                                                 uriAttribute: Option[Uri],
                                                                 readOnly: Boolean) extends InMemoryTask[T] {
    def load(projectId: Identifier)(implicit pluginContext: PluginContext): LoadedTask[T] = {
      def loadInternal(parameterValues: ParameterValues, pluginContext: PluginContext): Task[T] = {
        LoadedTask.success[T](PlainTask[TaskSpec](id, DatasetSpec[Dataset](pluginDesc(parameterValues)(pluginContext).asInstanceOf[Dataset],
          uriAttribute, readOnly), metaData).asInstanceOf[Task[T]])
      }

      LoadedTask.factory[T](loadInternal, parameters, pluginContext, Some(projectId), id, metaData.label, metaData.description)
    }
  }

  protected class InMemoryTemplateVariablesSerializer extends TemplateVariablesSerializer {

    @volatile
    private var variables = TemplateVariables.empty

    /**
      * Reads all variables at this scope.
      */
    override def readVariables()(implicit userContext: UserContext): TemplateVariables = {
      variables
    }

    /**
      * Updates all variables.
      */
    override def putVariables(variables: TemplateVariables)(implicit userContext: UserContext): Unit = {
      this.variables = variables
    }
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
