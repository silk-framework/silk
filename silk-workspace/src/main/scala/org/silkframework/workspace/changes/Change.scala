package org.silkframework.workspace.changes

import org.silkframework.config.{HasMetaData, TaskSpec}
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

  /** One line for display, the summary with the details, e.g. "Updated CSV dataset 'employees': Separator ',' → ';'". */
  def describe: String = {
    val shown = details.take(Change.maxDetails).map(_.describe)
    val more = if(details.size > Change.maxDetails) Seq(s"and ${details.size - Change.maxDetails} more") else Seq.empty
    if(details.isEmpty) summary else s"$summary: ${(shown ++ more).mkString(", ")}"
  }

  /** The change without its details, e.g. "Added value mapping 'name' (name → http://…/name) under 'root' in transform 'persons'". */
  def summary: String

  /** What the change changed where the summary does not tell: the parameters of a whole-task update or the fields of a mapping rule update with their values. */
  def details: Seq[ChangeDetail] = Seq.empty

  /** The change that undoes this one, or None if it cannot be undone, e.g. a file overwrite of which no copy was kept. */
  def inverse: Option[Change]

  /** Whether this change fulfils the given proposal, e.g. a workflow run the proposal to run it; recorded as [[ChangeEntry.fulfils]]. */
  def fulfils(proposal: Proposal): Boolean = false

  /**
    * Performs this change on the project through the regular write path, which records it in the journal.
    *
    * @throws ChangeConflictException If the project is not in the state this change expects.
    */
  def applyTo(project: Project)(implicit userContext: UserContext): Unit
}

/**
  * A change that is recorded instead of being applied, e.g. an agent's request to run a workflow: it waits for the
  * user's review, and the change that fulfils it links back to it ([[ChangeEntry.fulfils]]). Reverting a proposal
  * discards it; a fulfilled proposal is final.
  */
trait Proposal extends Change {

  final override def applyTo(project: Project)(implicit userContext: UserContext): Unit = {
    throw new IllegalStateException(s"A proposal is not applied as a change: $summary.")
  }
}

object Change {

  /** At most this many details go into [[Change.describe]]. */
  private val maxDetails = 5

  /** The label to capture in a change that does not hold the task itself; None when no label is set. */
  def capturedName(obj: HasMetaData): Option[String] = {
    obj.metaData.label.filter(_.trim.nonEmpty).map(_ => obj.labelOrId)
  }
}

/**
  * One thing a change changed, for display: what, and the value before and after where there is one to show.
  * Without values the label is the whole statement, e.g. "Password changed".
  */
case class ChangeDetail(label: String, before: Option[String] = None, after: Option[String] = None) {

  def describe: String = (before, after) match {
    case (Some(previous), Some(current)) => s"$label '$previous' → '$current'"
    case (None, Some(current)) => s"$label '$current' added"
    case (Some(previous), None) => s"$label '$previous' removed"
    case (None, None) => label
  }
}

/** A change recorded from the outcome of a write, e.g. a file write or a workflow run. It holds no content, so it is not applied itself. */
trait RecordedChange extends Change {

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = {
    throw new IllegalStateException(s"$changeType records the outcome of a write and cannot be applied.")
  }
}

/** A change that names the task it concerns, whether or not it changes the task's data. */
trait NamesTask {

  def taskId: Identifier

  /** The task's label, captured when the change was created; None when no label was set. */
  def taskLabel: Option[String]

  /** Names the task for display: the captured label, or the id. */
  final def taskName: String = taskLabel.getOrElse(taskId.toString)
}

/**
  * A change of the data of one task. A typed change carries only what it changes and [[apply]] is pure:
  * everything it needs, such as generated identifiers, is resolved before the change is created.
  *
  * @tparam T The task type this change applies to.
  */
abstract class TaskChange[T <: TaskSpec : ClassTag] extends Change with NamesTask {

  /** The task's label, captured when the change was created; None when no label was set. */
  override def taskLabel: Option[String] = None

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
    case other => throw ChangeConflictException(s"Task '$taskName' is a ${other.getClass.getSimpleName}, " +
      s"but this change expects a ${classTag[T].runtimeClass.getSimpleName}.")
  }

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = {
    val task = project.anyTaskOption(taskId)
      .getOrElse(throw ChangeConflictException(s"Task '$taskName' does not exist in project '${project.id}'."))
    task.applyChange(this)
  }
}

/** A change cannot be applied because the project is not in the state the change expects, e.g. it changed since. */
case class ChangeConflictException(msg: String) extends RequestException(msg, None) {

  override def errorTitle: String = "Change conflict"

  override def httpErrorCode: Option[Int] = Some(409)
}

/** The inverse of a change was applied, but left the project unchanged, so there is nothing that reverts it. */
case class ChangeNotRevertedException(msg: String) extends RequestException(msg, None) {

  override def errorTitle: String = "Change not reverted"

  override def httpErrorCode: Option[Int] = Some(409)
}
