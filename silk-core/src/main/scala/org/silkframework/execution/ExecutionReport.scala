package org.silkframework.execution

/**
  * Contains statistics about the execution of a task.
  */
trait ExecutionReport {

  /**
    * A user-friendly label for this report, usually just the task label.
    */
  def label: String

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
