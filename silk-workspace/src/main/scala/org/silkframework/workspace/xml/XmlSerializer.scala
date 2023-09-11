package org.silkframework.workspace.xml

import org.silkframework.config.TaskSpec.TaskSpecXmlFormat
import org.silkframework.config.{MetaData, Task, TaskSpec}
import org.silkframework.runtime.plugin.{ParameterValues, PluginContext}
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.runtime.serialization.{ReadContext, XmlFormat, XmlSerialization}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
import org.silkframework.workspace.{LoadedTask, TaskLoadingError}

import scala.reflect.ClassTag
import scala.util.control.NonFatal
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
  def removeTask(name: Identifier, resources: ResourceManager): Unit

  /**
    * Writes an updated task.
    *
    * @param resources The resource manager where the task will be written to.
    * @param projectResourceManager The resource manager to serialize project file resources correctly.
    */
  def writeTask(task: Task[TaskType], resources: ResourceManager, projectResourceManager: ResourceManager): Unit

  /**
    * Loads a file resource as XML.
    */
  protected def loadResourceAsXml(resources: ResourceLoader, resourceName: String): Elem = {
    val resource = resources.get(resourceName)
    try {
      resource.read(XML.load)
    } catch {
      case NonFatal(ex) =>
        throw new ValidationException(s"'${resource.path}' is not a valid XML file: " + ex.getMessage, ex)
    }
  }

  /**
    * Extracts task meta data from input stream and also returns the parsed XML element.
    */
  private def extractTaskMetaData(resources: ResourceLoader, resourceName: String)(implicit readContext: ReadContext): Try[(Option[MetaData], Elem)] = {
    Try {
      loadResourceAsXml(resources, resourceName)
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
                                      alternativeTaskId: Option[Identifier],
                                      resources: ResourceLoader)
                                     (implicit xmlFormat: XmlFormat[TaskType], context: PluginContext): LoadedTask[TaskType] = {
    implicit val readContext: ReadContext = ReadContext.fromPluginContext()
    loadTaskSafelyFromXML(extractTaskMetaData(resources, resourceName), resourceName, alternativeTaskId)
  }

  protected def loadTaskSafelyFromXML(taskXml: Elem,
                                      resourceName: String,
                                      alternativeTaskId: Option[Identifier],
                                      resources: ResourceLoader)
                                     (implicit xmlFormat: XmlFormat[TaskType], context: PluginContext): LoadedTask[TaskType] = {
    implicit val readContext: ReadContext = ReadContext.fromPluginContext()
    loadTaskSafelyFromXML(extractTaskMetaData(taskXml), resourceName, alternativeTaskId)
  }

  protected def loadTaskSafelyFromXML(taskElemWithMetaData: Try[(Option[MetaData], Elem)],
                                      resourceName: String,
                                      alternativeTaskId: Option[Identifier])
                                     (implicit xmlFormat: XmlFormat[TaskType],
                                      readContext: ReadContext): LoadedTask[TaskType] = {
    val taskOrError: Either[TaskLoadingError, Task[TaskType]] =
      taskElemWithMetaData match {
        case Success((metaData, elem)) =>
          (elem \ "@id").headOption.map(att => Identifier(att.text)).orElse(alternativeTaskId) match {
            case Some(taskId) =>
              def loadInternal(parameterValues: ParameterValues, pluginContext: PluginContext): Task[TaskType] = {
                implicit val taskXmlFormat: XmlFormat[Task[TaskType]] = Task.taskFormat[TaskType]
                val updatedReadContext = ReadContext.fromPluginContext()(pluginContext).copy(validationEnabled = readContext.validationEnabled, identifierGenerator = readContext.identifierGenerator)
                val newParameterValues = XmlSerialization.serializeParameters(parameterValues)
                // Nested parameters are not merged, but overwritten on the top level. As long as the complete parameter is sent and not a deep patch, this should be fine.
                val updatedElem = new Elem(elem.prefix, elem.label, elem.attributes, elem.scope, false, elem.child ++ newParameterValues : _*)
                XmlSerialization.fromXml[Task[TaskType]](updatedElem)(taskXmlFormat, updatedReadContext)
              }

              LoadedTask.factory[TaskType](loadInternal, ParameterValues(Map.empty), PluginContext.fromReadContext(readContext),
                readContext.projectId, taskId, metaData.flatMap(_.label), metaData.flatMap(_.description)).taskOrError
            case None =>
              // Nothing we can do here. XML element currently cannot be reloaded. This should only happen if a user actively removes the ID from the XML file.
              Left(TaskLoadingError(None, alternativeTaskId.getOrElse(s"No ID! Resource:$resourceName"),
                new RuntimeException(s"Could not find 'id' attribute in XML from file $resourceName${alternativeTaskId.map(n => s" for task '$n'").getOrElse("")}."),
                factoryFunction = None, originalParameterValues = None))
          }

        case Failure(ex) =>
          // Nothing we can do here. XML element currently cannot be reloaded. This should only happen if a user actively removes the ID from the XML file.
          Left(TaskLoadingError(None, alternativeTaskId.getOrElse(s"No ID! Resource:$resourceName"), ex, factoryFunction = None, originalParameterValues = None))
      }
    LoadedTask[TaskType](taskOrError)
  }
}
