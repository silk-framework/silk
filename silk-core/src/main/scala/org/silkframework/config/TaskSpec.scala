package org.silkframework.config

import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.plugin.{ParameterValues, PluginContext}
import org.silkframework.runtime.resource.Resource
import org.silkframework.runtime.serialization._
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier

import scala.xml.Node

/**
  * Base trait of all task specifications.
  */
@PluginType()
trait TaskSpec {

  /**
    * The input ports and their schemata.
    */
  def inputPorts: InputPorts

  /**
    * The output port and it's schema.
    * None, if this operator does not generate any output.
    */
  def outputPort: Option[Port]

  /**
    * The tasks that this task reads from.
    */
  def inputTasks: Set[Identifier] = Set.empty

  /**
    * The tasks that this task writes to.
    */
  def outputTasks: Set[Identifier] = Set.empty

  /**
    * The tasks that are directly referenced by this task.
    * This includes input tasks and output tasks.
    */
  def referencedTasks: Set[Identifier] = inputTasks ++ outputTasks

  /**
    * The resources that are directly referenced by this task.
    */
  def referencedResources: Seq[Resource] = Seq.empty

  /**
   * Called if a referenced resource has been updated.
   * The calls are done on a best-effort basis and there is not guarantee that this method is called for all updates.
   * In particular, it is only called if the resource is updated via Silk/DataIntegration and not for external updates.
   */
  def resourceUpdated(resource: Resource): Unit = {
    // Overwrite to handle updates
  }

  /**
    * Retrieves all parameter values for this task.
    */
  def parameters(implicit pluginContext: PluginContext): ParameterValues

  /**
    * Creates a new instance of this task with updated parameters.
    *
    * @param updatedParameters A list of parameter values to be updated.
    *                          This can be a subset of all available parameters.
    *                          Parameter values that are not provided remain unchanged.
    * @param dropExistingValues If true, the caller is expected to provide values for all parameters.
    *                           If false, the updated parameters can be a subset of all available parameters.
    */
  def withParameters(updatedParameters: ParameterValues, dropExistingValues: Boolean = false)(implicit context: PluginContext): TaskSpec

  /** Related links for this task that can be presented to the user. */
  def taskLinks: Seq[TaskLink] = Seq.empty

  /** The main (execution) activities that should stand out from all the registered activities of this task. */
  def mainActivities: Seq[String] = Seq.empty

  /** Additional tags that will be displayed in the UI for this task. These tags are covered by the workspace search. */
  def searchTags: Seq[String] = Seq.empty
}

/** A task link.
  *
  * @param id  The ID of the link.
  * @param url The absolute URL of the link.
  */
case class TaskLink(id: String, url: String)

object TaskSpec {

  implicit object TaskSpecXmlFormat extends XmlFormat[TaskSpec] {

    // Holds all XML formats for sub classes of TaskSpec.
    private lazy val taskSpecFormats: Seq[XmlFormat[TaskSpec]] = {
      Serialization.availableFormats.filter(f => f.isInstanceOf[XmlFormat[_]] && classOf[TaskSpec].isAssignableFrom(f.valueType) && f.valueType != classOf[TaskSpec])
        .map(_.asInstanceOf[XmlFormat[TaskSpec]])
    }

    override def read(value: Node)(implicit readContext: ReadContext): TaskSpec = {
      val tagName = value.label
      taskSpecFormats.find(_.tagNames.contains(tagName)) match {
        case Some(format) =>
          format.read(value)
        case None =>
          throw new ValidationException(s"The encountered tag name $tagName does not correspond to a known task type")
      }
    }

    override def write(value: TaskSpec)(implicit writeContext: WriteContext[Node]): Node = {
      taskSpecFormats.find(_.valueType.isAssignableFrom(value.getClass)) match {
        case Some(format) =>
          format.write(value)
        case None =>
          throw new ValidationException(s"No serialization format found for class ${value.getClass.getName}")
      }
    }
  }
}
