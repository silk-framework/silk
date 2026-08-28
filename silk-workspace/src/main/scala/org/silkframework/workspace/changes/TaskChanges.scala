package org.silkframework.workspace.changes

import org.silkframework.config.{PlainTask, Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, ProjectTask}

/** Adds a task to the project. Recorded whenever a task is added. */
case class AddTask(task: PlainTask[TaskSpec]) extends Change {

  override def describe: String = s"Added task '${task.id}'"

  override def inverse: Option[RemoveTask] = Some(RemoveTask(task))

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = {
    if(project.anyTaskOption(task.id).isDefined) {
      throw ChangeConflictException(s"Task '${task.id}' already exists in project '${project.id}'.")
    }
    project.addAnyTask(task.id, task.data, task.metaData, task.executionVariables)
  }

  // The task parameters may be sensitive, so they are never printed.
  override def toString: String = s"AddTask(${task.id})"
}

/** Removes a task from the project. Holds the removed task, so the removal can be reverted. */
case class RemoveTask(task: PlainTask[TaskSpec]) extends Change {

  override def describe: String = s"Removed task '${task.id}'"

  override def inverse: Option[AddTask] = Some(AddTask(task))

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = {
    TaskChanges.expectState(project, task)
    project.removeAnyTask(task.id, removeDependentTasks = false)
  }

  override def toString: String = s"RemoveTask(${task.id})"
}

/**
  * Replaces a whole task. Recorded when a task is updated without a typed change, e.g. by a whole-task save.
  * Reverting requires the task to be unchanged since, as the whole task is restored.
  */
case class ReplaceTask(before: PlainTask[TaskSpec], after: PlainTask[TaskSpec]) extends Change {

  def taskId: Identifier = before.id

  override def describe: String = s"Updated task '$taskId'"

  override def inverse: Option[ReplaceTask] = Some(ReplaceTask(after, before))

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = {
    val task = TaskChanges.expectState(project, before)
    // Timestamps and users are dropped, so the update is stamped as a new modification.
    task.update(after.data, Some(after.metaData.withoutUserData), Some(after.executionVariables))
  }

  override def toString: String = s"ReplaceTask($taskId)"
}

object TaskChanges {

  /** Whether two tasks hold the same data, execution variables and metadata, ignoring timestamps and users. */
  def same(task1: Task[TaskSpec], task2: Task[TaskSpec]): Boolean = {
    task1.data == task2.data &&
      task1.metaData.withoutUserData == task2.metaData.withoutUserData &&
      task1.executionVariables == task2.executionVariables
  }

  /** The project task in the state of `expected`; throws a conflict if it is missing or has changed since. */
  private[changes] def expectState(project: Project, expected: PlainTask[TaskSpec])
                                  (implicit userContext: UserContext): ProjectTask[TaskSpec] = {
    val task = project.anyTaskOption(expected.id)
      .getOrElse(throw ChangeConflictException(s"Task '${expected.id}' does not exist in project '${project.id}'."))
    if(!same(task, expected)) {
      throw ChangeConflictException(s"Task '${expected.id}' in project '${project.id}' has been changed since.")
    }
    task.asInstanceOf[ProjectTask[TaskSpec]]
  }
}
