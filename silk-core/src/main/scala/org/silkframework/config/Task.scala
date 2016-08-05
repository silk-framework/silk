package org.silkframework.config

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.util.Identifier

import scala.language.implicitConversions
import scala.xml._

/**
  * A task, such as a dataset or a transformation task.
  *
  * @tparam TaskType The type of this task, e.g., TransformSpec.
  */
trait Task[+TaskType <: TaskSpec] {
  /** The id of this task. */
  def id: Identifier

  /** The task specification that holds the actual task specification. */
  def data: TaskType

  /**
    * Returns this task as a [[Task]]. For some reason the type inference mechanism of Scala is not able
    * to infer that this is a Task[TaskType] for implicits if this is a subclass of [[Task]] So this conversion must be done there.
    */
  def taskTrait: Task[TaskType] = this.asInstanceOf[Task[TaskType]]
}

case class PlainTask[+TaskType <: TaskSpec](id: Identifier, data: TaskType) extends Task[TaskType]

object Task {

  /**
    * Implicitly converts a task to its specification.
    */
  implicit def taskData[T <: TaskSpec](task: Task[T]): T = task.data

  /**
    * Returns the xml serialization format for a Task.
    *
    * @param xmlFormat The xml serialization format for type T.
    */
  implicit def taskFormat[T <: TaskSpec](implicit xmlFormat: XmlFormat[T]): XmlFormat[Task[T]] = new TaskFormat[T]

  /**
    * XML serialization format.
    */
  private class TaskFormat[T <: TaskSpec](implicit xmlFormat: XmlFormat[T]) extends XmlFormat[Task[T]] {
    /**
      * Deserialize a value from XML.
      */
    def read(node: Node)(implicit readContext: ReadContext) = {
      PlainTask(
        id = (node \ "@id").text,
        data = XmlSerialization.fromXml[T](node)
      )
    }

    /**
      * Serialize a value to XML.
      */
    def write(task: Task[T])(implicit writeContext: WriteContext[Node]): Node = {
      var node = XmlSerialization.toXml(task.data).head.asInstanceOf[Elem]
      node = node % Attribute("id", Text(task.id), Null)
      node
    }
  }
}