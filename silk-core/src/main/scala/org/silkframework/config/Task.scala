package org.silkframework.config

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.util.Identifier

import scala.language.implicitConversions
import scala.reflect.ClassTag
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

  /** Meta data about this task. */
  def metaData: MetaData

  /**
    * Type of task data.
    */
  def taskType: Class[_]

  /**
    * Returns this task as a [[Task]]. For some reason the type inference mechanism of Scala is not able
    * to infer that this is a Task[TaskType] for implicits if this is a subclass of [[Task]] So this conversion must be done there.
    */
  def taskTrait: Task[TaskType] = this.asInstanceOf[Task[TaskType]]

  /**
    * Finds all tasks that reference this task.
    *
    * @param recursive Whether to return tasks that indirectly refer to this task.
    */
  def findDependentTasks(recursive: Boolean)
                        (implicit userContext: UserContext): Set[Identifier] = Set.empty

  /**
    * Returns the label if defined or the task ID. Truncates the label to maxLength characters.
    * @param maxLength the max length in characters
    */
  def taskLabel(maxLength: Int = MetaData.DEFAULT_LABEL_MAX_LENGTH): String = {
    metaData.formattedLabel(id, maxLength)
  }

  override def equals(obj: scala.Any) = obj match {
    case task: Task[_] =>
      id == task.id &&
      data == task.data &&
      metaData == task.metaData
    case _ =>
      false
  }
}

case class PlainTask[+TaskType <: TaskSpec : ClassTag](id: Identifier, data: TaskType, metaData: MetaData = MetaData.empty) extends Task[TaskType] {

  override def taskType: Class[_] = implicitly[ClassTag[TaskType]].runtimeClass

}

object PlainTask {
  def fromTask[T <: TaskSpec : ClassTag](task: Task[T]): PlainTask[T] = {
    PlainTask(task.id, task.data, task.metaData)
  }
}

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
  implicit def taskFormat[T <: TaskSpec : ClassTag](implicit xmlFormat: XmlFormat[T]): XmlFormat[Task[T]] = new TaskFormat[T]

  /**
    * Enables pattern matching over tasks.
    */
  def unapply[T <: TaskSpec](task: Task[T]): Option[(Identifier, T)] = {
    Some(task.id, task.data)
  }

  /**
    * XML serialization format.
    */
  class TaskFormat[T <: TaskSpec : ClassTag](implicit xmlFormat: XmlFormat[T]) extends XmlFormat[Task[T]] {

    import XmlSerialization._

    /**
      * Deserialize a value from XML.
      */
    def read(node: Node)(implicit readContext: ReadContext) = {
      PlainTask(
        id = (node \ "@id").text,
        data = fromXml[T](node),
        metaData = (node \ "MetaData").headOption.map(fromXml[MetaData]).getOrElse(MetaData.empty)
      )
    }

    /**
      * Serialize a value to XML.
      */
    def write(task: Task[T])(implicit writeContext: WriteContext[Node]): Node = {
      var node = toXml(task.data).head.asInstanceOf[Elem]
      node = node % Attribute("id", Text(task.id), Null)
      node = node.copy(child = toXml[MetaData](task.metaData) +: node.child)
      node
    }
  }

  implicit object GenericTaskFormat extends TaskFormat[TaskSpec]
}