package org.silkframework.execution

/**
  * Contains statistics about the execution of a task.
  */
trait ExecutionReport {

  /**
    * Generates a short summary of this report.
    *
    * @return A sequence of key-value pairs representing the summary table.
    */
  def summary: Seq[(String, String)]

}
