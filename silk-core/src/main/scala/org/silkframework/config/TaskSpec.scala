package org.silkframework.config

import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.serialization.{ReadContext, Serialization, WriteContext, XmlFormat}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier

import scala.xml.Node

/**
  * Base trait of all task specifications.
  */
trait TaskSpec {

  /**
    * The schemata of the input data for this task.
    * A separate entity schema is returned for each input.
    * Or None is returned, which means that this task can handle any number of inputs and any kind
    * of entity schema.
    * A result of Some(Seq()) on the other hand means that this task has no inputs at all.
    */
  def inputSchemataOpt: Option[Seq[EntitySchema]]

  /**
    * The schema of the output data.
    * Returns None, if the schema is unknown or if no output is written by this task.
    */
  def outputSchemaOpt: Option[EntitySchema]

  /**
    * The tasks that are referenced by this task.
    */
  def referencedTasks: Set[Identifier] = Set.empty

}

object TaskSpec {

  implicit object TaskSpecXmlFormat extends XmlFormat[TaskSpec] {

    // Holds all XML formats for sub classes of TaskSpec.
    private lazy val taskSpecFormats: Seq[XmlFormat[TaskSpec]] = {
      Serialization.availableFormats.filter(f => f.isInstanceOf[XmlFormat[_]] && classOf[TaskSpec].isAssignableFrom(f.valueType) && f != this)
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
