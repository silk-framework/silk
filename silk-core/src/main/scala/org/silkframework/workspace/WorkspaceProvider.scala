package org.silkframework.workspace

import org.silkframework.config.{MetaData, PlainTask, Prefixes, Tag, Task, TaskSpec}
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.plugin.{AnyPlugin, ParameterValues, PluginContext}
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.util.Identifier
import org.silkframework.workspace.resources.ResourceRepository

import scala.collection.mutable
import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.util.control.NonFatal
import scala.xml.{Attribute, Elem, Node, Text, Null}

@PluginType()
trait WorkspaceProvider extends AnyPlugin {

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
    * Imports a project.
    *
    * @param project The meta data of the project to be imported.
    * @param importProvider The workspace provider that contains the project to be imported.
    * @param importResources The resources of the project to be imported.
    * @param targetResources The resources of the new project in this workspace.
    * @param alsoCopyResources If true, the project resources are imported as well.
    *                          Otherwise, only the tasks ore imported.
    */
  def importProject(project: ProjectConfig,
                    importProvider: WorkspaceProvider,
                    importResources: ResourceManager,
                    targetResources: ResourceManager,
                    alsoCopyResources: Boolean)
                   (implicit user: UserContext): Unit

  /**
   * Retrieves the project cache folder.
   */
  def projectCache(name: Identifier): ResourceManager

  /**
    * Reads all tasks of a specific type from a project.
    **/
  def readTasks[T <: TaskSpec : ClassTag](project: Identifier)
                                         (implicit context: PluginContext): Seq[LoadedTask[T]]

  /**
    * Reads all tasks of all types from a project.
    **/
  def readAllTasks(project: Identifier)
                  (implicit context: PluginContext): Seq[LoadedTask[_]]

  /**
   * Adds/Updates a task in a project.
    *
    * @param projectResourceManager The resource manager that is used to serialize the path of project resources correctly.
   */
  def putTask[T <: TaskSpec : ClassTag](project: Identifier,
                                        task: Task[T],
                                        resources: ResourceManager)
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
    * Refreshes a single project, i.e. cleans all possible caches if there are any and reloads the project freshly.
    */
  def refreshProject(project: Identifier, projectResources: ResourceManager)(implicit userContext: UserContext): Unit = { }

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
    taskOrError.getOrElse(throw taskOrError.left.get.throwable)
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

  /** Returns a loaded task with properly set task factory function in case of a loading failure.
    *
    * @param taskFactory           A factory function that takes parameter values that will be merged with the original parameters
    *                              and an alternative plugin context.
    * @param originalParameters    The original parameters.
    * @param originalPluginContext The original plugin context.
    */
  def factory[T <: TaskSpec : ClassTag](taskFactory: (ParameterValues, PluginContext) => Task[T],
                                        originalParameters: ParameterValues,
                                        originalPluginContext: PluginContext,
                                        projectId: Option[Identifier],
                                        taskId: Identifier,
                                        label: Option[String] = None,
                                        description: Option[String] = None): LoadedTask[T] = {
    def loadInternal(parameterValueOverwrites: ParameterValues, alternativePluginContext: PluginContext): LoadedTask[T] = {
      try {
        val mergedParameters = originalParameters.merge(parameterValueOverwrites)
        val task = taskFactory(mergedParameters, alternativePluginContext)
        LoadedTask.success[T](task)
      } catch {
        case ex: TaskLoadingException =>
          LoadedTask.failed[T](TaskLoadingError(projectId, taskId, ex.cause, label, description, Some(loadInternal), Some(ex.originalTaskData)))
        case NonFatal(ex) =>
          LoadedTask.failed[T](TaskLoadingError(projectId, taskId, ex, label, description, Some(loadInternal), None))
      }
    }
    loadInternal(originalParameters, originalPluginContext)
  }

  implicit def convertTaskToLoadedTask[T <: TaskSpec : ClassTag](task: Task[T]): LoadedTask[T] = LoadedTask.success(task)
  implicit def convertLoadedTaskToTask[T <: TaskSpec : ClassTag](loadedTask: LoadedTask[T]): Task[T] = {
    loadedTask.taskOrError match {
      case Right(task) => task
      case Left(error) => throw error.throwable
    }
  }

  /**
    * Returns the xml serialization format for a LoadedTask.
    *
    * @param xmlFormat The xml serialization format for type T.
    */
  implicit def loadedTaskFormat[T <: TaskSpec : ClassTag](implicit xmlFormat: XmlFormat[T]): XmlFormat[LoadedTask[T]] = new LoadedTaskFormat[T]

  /**
    * XML serialization format.
    */
  class LoadedTaskFormat[T <: TaskSpec : ClassTag](implicit xmlFormat: XmlFormat[T]) extends XmlFormat[LoadedTask[T]] {

    import XmlSerialization._

    /**
      * Deserialize a value from XML.
      */
    def read(node: Node)(implicit readContext: ReadContext): LoadedTask[T] = {
      LoadedTask(
        Right(PlainTask(
          id = (node \ "@id").text,
          data = fromXml[T](node),
          metaData = (node \ "MetaData").headOption.map(fromXml[MetaData]).getOrElse(MetaData.empty)
        ))
      )
    }

    /**
      * Serialize a value to XML.
      */
    def write(task: LoadedTask[T])(implicit writeContext: WriteContext[Node]): Node = {
      var node = toXml(task.data).head.asInstanceOf[Elem]
      node = node % Attribute("id", Text(task.id), Null)
      node = node.copy(child = toXml[MetaData](task.metaData) +: node.child)
      node
    }
  }
}

/** Task loading error.
  *
  * @param projectId       ID of the project the task is located in.
  * @param taskId          ID of the task.
  * @param throwable       The exception that was thrown.
  * @param label           Optional label of the task for display purposes.
  * @param description     Optional description of the task for display purposes.
  * @param factoryFunction Optional function that tries to create the task instance.
  * @param originalParameterValues The original parameter values, if those could extracted.
  */
case class TaskLoadingError(projectId: Option[Identifier],
                            taskId: Identifier,
                            throwable: Throwable,
                            label: Option[String] = None,
                            description: Option[String] = None,
                            factoryFunction: Option[(ParameterValues, PluginContext) => LoadedTask[_ <: TaskSpec]],
                            originalParameterValues: Option[OriginalTaskData])

/** Data necessary to restore a task that has failed loading. */
case class OriginalTaskData(pluginId: String,
                            parameterValues: ParameterValues)

/** An exception that carries the original parameter (simple parameters only!) values of a task if available.
  * Do not use directly. Use withTaskLoadingException instead. */
case class TaskLoadingException(msg: String, originalTaskData: OriginalTaskData, cause: Throwable) extends RuntimeException(msg, cause)

object TaskLoadingException {
  /** This should be used where a TaskLoadingException should be thrown. */
  def withTaskLoadingException[T](getOriginalTaskData: => OriginalTaskData)(create: ParameterValues => T): T = {
    val originalTaskData = getOriginalTaskData
    try {
      create(originalTaskData.parameterValues)
    } catch {
      case ex: TaskLoadingException =>
        // TaskLoadingException was thrown, just pass on
        throw ex
      case NonFatal(ex) =>
        throw TaskLoadingException("Task has failed loading.", originalTaskData, ex)
    }
  }
}