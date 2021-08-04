package org.silkframework.workspace.activity.workflow

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.execution.ExecutionReport
import org.silkframework.util.Identifier

/**
  * A workflow execution report.
  *
  * @param task The workflow that was executed
  * @param taskReports A map from each workflow operator id to its corresponding report
  */
case class WorkflowExecutionReport(task: Task[TaskSpec], taskReports: IndexedSeq[WorkflowTaskReport] = IndexedSeq.empty) extends ExecutionReport {

  /**
    * Adds a new task report.
    *
    * @return The updated workflow report
    */
  def addReport(nodeId: Identifier, report: ExecutionReport): WorkflowExecutionReport = {
    copy(taskReports = taskReports :+ WorkflowTaskReport(nodeId, report))
  }

  /**
    * Updates an existing task report.
    *
    * @return The updated workflow report
    */
  def updateReport(index: Int, nodeId: Identifier, report: ExecutionReport): WorkflowExecutionReport = {
    if(index < taskReports.size) {
      copy(taskReports = taskReports.updated(index, WorkflowTaskReport(nodeId, report)))
    } else {
      throw new IndexOutOfBoundsException(s"Invalid task report index: $index")
    }
  }

  override def summary: Seq[(String, String)] = Seq.empty

  override def warnings: Seq[String] = {
    if(taskReports.exists(_.report.warnings.nonEmpty)) {
      Seq("Some tasks generated warnings.")
    } else {
      Seq.empty
    }
  }
}

/**
  * Report of a single workflow operator execution.
  *
  * @param nodeId The node identifier within the workflow
  * @param report The execution report.
  */
case class WorkflowTaskReport(nodeId: Identifier, report: ExecutionReport)
