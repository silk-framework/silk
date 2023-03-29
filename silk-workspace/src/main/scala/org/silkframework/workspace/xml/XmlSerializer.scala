package org.silkframework.workspace.xml

import org.silkframework.config.{MetaData, Task, TaskSpec}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.runtime.serialization.{ReadContext, XmlFormat, XmlSerialization}
import org.silkframework.util.Identifier
import org.silkframework.workspace.{LoadedTask, TaskLoadingError}

import java.io.InputStream
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, XML}

/**
 * A plugin that adds a new module to the workspace.
 */
private abstract class XmlSerializer[TaskType <: TaskSpec : ClassTag] {

  /**
    * A prefix that uniquely identifies this module.
    */
  def prefix: String

  /**
    * Loads all tasks of this module in a safe way. Invalid tasks can be handled separately this way.
    */
  def loadTasks(resources: ResourceLoader)
               (implicit context: PluginContext): Seq[LoadedTask[TaskType]]

  /**
    * Removes a specific task.
    */
  def removeTask(name: Identifier, resources: ResourceManager)

  /**
    * Writes an updated task.
    *
    * @param resources The resource manager where the task will be written to.
    * @param projectResourceManager The resource manager to serialize project file resources correctly.
    */
  def writeTask(task: Task[TaskType], resources: ResourceManager, projectResourceManager: ResourceManager)

  /**
    * Extracts task meta data from input stream and also returns the parsed XML element.
    */
  private def extractTaskMetaData(is: InputStream)(implicit readContext: ReadContext): Try[(Option[MetaData], Elem)] = {
    Try {
      XML.load(is)
    } flatMap { elem =>
      extractTaskMetaData(elem)
    }
  }

  private def extractTaskMetaData(elem: Elem)
                                 (implicit readContext: ReadContext): Try[(Option[MetaData], Elem)] = {
    Try {
      (elem \ "MetaData").headOption match {
        case Some(metaDataNode) =>
          val metaData = MetaData.MetaDataXmlFormat.read(metaDataNode)
          (Some(metaData), elem)
        case None =>
          (None, elem)
      }
    }
  }

  /** Safely loads a task from an XML resource.
    * Reports errors on invalid XML and on invalid expected structure for parsing the task.
    *
    * @param resourceName The name of the resource with the serialized task XML content.
    * @param resources    The resource loader from which the resource should be loaded from.
    */
  protected def loadTaskSafelyFromXML(resourceName: String,
                                      alternativeTaskId: Option[String],
                                      resources: ResourceLoader)
                                     (implicit xmlFormat: XmlFormat[TaskType], context: PluginContext): LoadedTask[TaskType] = {
    implicit val readContext = ReadContext.fromPluginContext()
    val resource = resources.get(resourceName)
    loadTaskSafelyFromXML(extractTaskMetaData(resource.inputStream), resourceName, alternativeTaskId)
  }

  protected def loadTaskSafelyFromXML(taskXml: Elem,
                                      resourceName: String,
                                      alternativeTaskId: Option[String],
                                      resources: ResourceLoader)
                                     (implicit xmlFormat: XmlFormat[TaskType], context: PluginContext): LoadedTask[TaskType] = {
    implicit val readContext = ReadContext.fromPluginContext()
    loadTaskSafelyFromXML(extractTaskMetaData(taskXml), resourceName, alternativeTaskId)
  }

  protected def loadTaskSafelyFromXML(taskElemWithMetaData: Try[(Option[MetaData], Elem)],
                                      resourceName: String,
                                      alternativeTaskId: Option[String])
                                     (implicit xmlFormat: XmlFormat[TaskType],
                                      readContext: ReadContext): LoadedTask[TaskType] = {
    val taskOrError =
      taskElemWithMetaData match {
        case Success((metaData, elem)) =>
          (elem \ "@id").headOption.map(_.text).orElse(alternativeTaskId) match {
            case Some(taskId) =>
              Try(XmlSerialization.fromXml[Task[TaskType]](elem)) match {
                case Success(task) => Right(task)
                case Failure(ex) =>
                  Left(TaskLoadingError(None, taskId, ex, label = metaData.flatMap(_.label), description = metaData.flatMap(_.description)))
              }
            case None =>
              Left(TaskLoadingError(None, alternativeTaskId.getOrElse(s"No ID! Resource:$resourceName"),
                new RuntimeException(s"Could not find 'id' attribute in XML from file $resourceName${alternativeTaskId.map(n => s" for task '$n'").getOrElse("")}.")))
          }

        case Failure(ex) =>
          Left(TaskLoadingError(None, alternativeTaskId.getOrElse(s"No ID! Resource:$resourceName"), ex))
      }
    LoadedTask[TaskType](taskOrError)
  }
}
