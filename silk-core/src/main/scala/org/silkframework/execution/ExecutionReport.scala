package org.silkframework.execution

import org.silkframework.config.{Task, TaskSpec}

/**
  * Contains statistics about the execution of a task.
  */
trait ExecutionReport {

  /**
    * The task that corresponds to this report.
    */
  def task: Task[TaskSpec]

  /**
    * A user-friendly label for this report, usually just the task label.
    */
  def label: String = task.taskLabel()

  /**
    * Short label for the operation thas has been executed, e.g., read or write.
    */
  def operation: Option[String] = None

  /**
    * Generates a short summary of this report.
    *
    * @return A sequence of key-value pairs representing the summary table.
    */
  def summary: Seq[(String, String)]

  /**
    * If issues occurred during execution, this contains a list of user-friendly messages.
    */
  def warnings: Seq[String]

}
