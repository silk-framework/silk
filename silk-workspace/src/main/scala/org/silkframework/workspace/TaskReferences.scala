package org.silkframework.workspace

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.validation.RequestException
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.Workflow

/** A task that references another. */
case class ReferencingTask(task: ProjectTask[_ <: TaskSpec], referenced: Identifier) {

  /** How the task refers to the other (as input, as output, ...), so a rejected deletion says where to look. */
  def describe: String = {
    val kinds = Seq(
      Option.when(task.data.inputTasks.contains(referenced))("as input"),
      Option.when(task.data.outputTasks.contains(referenced))("as output")
    ).flatten
    if(kinds.nonEmpty) {
      kinds.mkString(" and ")
    } else task.data match {
      // Sources and sinks were matched above, so the node sits on the canvas without connections.
      case _: Workflow => "as a workflow node without connections"
      case _ => "in its rules or configuration"
    }
  }
}

/** What counts as a reference between tasks: what a removal refuses for, and what the change journal checks a removal against. */
object TaskReferences {

  /** The tasks that reference each task, by the id of the referenced task; sorted by id. */
  def of(tasks: Seq[ProjectTask[_ <: TaskSpec]]): Map[Identifier, Seq[ReferencingTask]] = {
    val references = for(task <- tasks; referenced <- task.data.referencedTasks.toSeq) yield ReferencingTask(task, referenced)
    references.groupMap(_.referenced)(identity).view.mapValues(_.sortBy(_.task.id.toString)).toMap
  }
}

/**
  * A task is not removed, as other tasks reference it; answers 409. Names the referencing tasks and, as the blast
  * radius, every task that removing it together with its dependents would delete.
  */
case class TaskReferencedException(taskName: Identifier, referencingTasks: Seq[ReferencingTask], dependentTasks: Seq[Identifier])
  extends RequestException(TaskReferencedException.message(taskName, referencingTasks, dependentTasks), None) {

  override def errorTitle: String = "Conflict"

  override def httpErrorCode: Option[Int] = Some(409)
}

object TaskReferencedException {

  private def message(taskName: Identifier, referencingTasks: Seq[ReferencingTask], dependentTasks: Seq[Identifier]): String = {
    val references = referencingTasks.map(r => s"${r.task.id} (${r.describe})").mkString(", ")
    s"Cannot delete task $taskName as it is referenced by task${if(referencingTasks.size > 1) "s" else ""} $references. " +
      s"Pass removeDependentTasks=true to delete it together with all tasks that depend on it: ${dependentTasks.map(_.toString).sorted.mkString(", ")}."
  }
}
