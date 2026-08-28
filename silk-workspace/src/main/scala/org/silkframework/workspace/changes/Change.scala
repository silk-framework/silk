package org.silkframework.workspace.changes

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.RequestException
import org.silkframework.util.Identifier
import org.silkframework.workspace.Project

import scala.reflect.{ClassTag, classTag}

/**
  * A change of a project, as recorded in its [[ChangeJournal]].
  * A change knows its inverse, so reverting a change is applying its inverse through the regular write path.
  */
trait Change {

  /** The kind of change, as served by the API. */
  def changeType: String = getClass.getSimpleName

  /** Short description for display, e.g. "Added mapping rule 'name' under 'root' in transform 'persons'". */
  def describe: String

  /** The change that undoes this one, or None if it cannot be undone, e.g. a file overwrite of which no copy was kept. */
  def inverse: Option[Change]

  /**
    * Performs this change on the project through the regular write path, which records it in the journal.
    *
    * @throws ChangeConflictException If the project is not in the state this change expects.
    */
  def applyTo(project: Project)(implicit userContext: UserContext): Unit
}

/**
  * A change of the data of one task. A typed change carries only what it changes and [[apply]] is pure:
  * everything it needs, such as generated identifiers, is resolved before the change is created.
  *
  * @tparam T The task type this change applies to.
  */
abstract class TaskChange[T <: TaskSpec : ClassTag] extends Change {

  def taskId: Identifier

  /**
    * Applies this change to the task data.
    *
    * @throws ChangeConflictException If the data is not in the state this change expects.
    */
  def apply(data: T): T

  override def inverse: Option[TaskChange[T]]

  /** Applies this change to task data of unknown type. */
  final def applyAny(data: TaskSpec): TaskSpec = data match {
    case typed: T => apply(typed)
    case other => throw ChangeConflictException(s"Task '$taskId' is a ${other.getClass.getSimpleName}, " +
      s"but this change expects a ${classTag[T].runtimeClass.getSimpleName}.")
  }

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = {
    val task = project.anyTaskOption(taskId)
      .getOrElse(throw ChangeConflictException(s"Task '$taskId' does not exist in project '${project.id}'."))
    task.applyChange(this)
  }
}

/** A change cannot be applied because the project is not in the state the change expects, e.g. it changed since. */
case class ChangeConflictException(msg: String) extends RequestException(msg, None) {

  override def errorTitle: String = "Change conflict"

  override def httpErrorCode: Option[Int] = Some(409)
}
