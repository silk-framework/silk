package org.silkframework.config

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.util.Identifier
import org.silkframework.workspace.LoadedTask

import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.xml.{Attribute, Elem, Node, Null, Text}

/**
  * A task, such as a dataset or a transformation task.
  *
  * @tparam TaskType The type of this task, e.g., TransformSpec.
  */
trait Task[+TaskType <: TaskSpec] extends HasMetaData {
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
    * @param ignoreTasks Set of tasks to be ignored in the dependency search.
    */
  def findDependentTasks(recursive: Boolean, ignoreTasks: Set[Identifier] = Set.empty)
                        (implicit userContext: UserContext): Set[Identifier] = Set.empty

  /** Find tasks that are either input or output to this task. */
  def findRelatedTasksInsideWorkflows()(implicit userContext: UserContext): Set[Identifier] = Set.empty

  override def equals(obj: scala.Any): Boolean = obj match {
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
    def read(node: Node)(implicit readContext: ReadContext): Task[T] = {
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