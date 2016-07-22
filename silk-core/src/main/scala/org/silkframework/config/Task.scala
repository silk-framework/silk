package org.silkframework.config

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.util.Identifier
import scala.xml._

case class Task[+TaskType <: TaskSpecification](id: Identifier, data: TaskType) {

}

object Task {

  /**
    * Implicitly converts a task to its specification.
    */
  implicit def taskData[T <: TaskSpecification](task: Task[T]): T = task.data

  /**
    * Returns the xml serialization format for a Task.
    *
    * @param xmlFormat The xml serialization format for type T.
    */
  implicit def taskFormat[T <: TaskSpecification](implicit xmlFormat: XmlFormat[T]): XmlFormat[Task[T]] = new TaskFormat[T]

  /**
    * XML serialization format.
    */
  private class TaskFormat[T <: TaskSpecification](implicit xmlFormat: XmlFormat[T]) extends XmlFormat[Task[T]] {
    /**
      * Deserialize a value from XML.
      */
    def read(node: Node)(implicit readContext: ReadContext) = {
      Task(
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