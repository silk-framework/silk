package org.silkframework.workspace.activity.workflow

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.execution.{ExecutionReport, SimpleExecutionReport}
import org.silkframework.util.Identifier

import java.time.Instant

/**
  * A workflow execution report.
  *
  * @param task The workflow that was executed
  * @param taskReports A map from each workflow operator id to its corresponding report
  */
case class WorkflowExecutionReport(task: Task[TaskSpec], taskReports: IndexedSeq[WorkflowTaskReport] = IndexedSeq.empty, isDone: Boolean = false) extends ExecutionReport {

  /**
    * Retrieves all task reports for a given workflow node id.
    */
  def retrieveReports(nodeId: Identifier): Seq[ExecutionReport] = {
    val reports = taskReports.filter(_.nodeId == nodeId).map(_.report)
    if(reports.nonEmpty) {
      reports
    } else {
      throw new NoSuchElementException(s"No report for node '$nodeId' found.")
    }
  }

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

  /**
    * Updates a task report if a node failed.
    *
    * @return The updated workflow report
    */
  def addFailedNode(nodeId: Identifier, ex: Throwable): WorkflowExecutionReport = {
    taskReports.reverse.zipWithIndex.find(_._1.nodeId == nodeId) match {
      case Some((workflowReport, index)) =>
        val report = workflowReport.report
        val errorMsg = ex.getMessage
        val errorReport = SimpleExecutionReport(report.task, report.summary, report.warnings, Some(errorMsg), isDone = true, report.entityCount, report.operation)
        copy(taskReports = taskReports.updated(index, WorkflowTaskReport(nodeId, errorReport)))
      case None =>
        throw new NoSuchElementException(s"Invalid task node identifier: $nodeId")
    }
  }

  /**
    * Returns a copy of this report in which itself and all its task reports are marked as done.
    *
    * @return The updated workflow report
    */
  def asDone(): WorkflowExecutionReport = {
    copy(taskReports = taskReports.map(r => r.copy(report = r.report.asDone())), isDone = true)
  }

  override def summary: Seq[(String, String)] = Seq.empty

  override def warnings: Seq[String] = {
    if(taskReports.exists(_.report.warnings.nonEmpty)) {
      Seq("Some tasks generated warnings.")
    } else {
      Seq.empty
    }
  }

  override def entityCount: Int = taskReports.size
}

/**
  * Report of a single workflow operator execution.
  *
  * @param nodeId The node identifier within the workflow
  * @param report The execution report.
  * @param timestamp Timestamp of the last update.
  */
case class WorkflowTaskReport(nodeId: Identifier, report: ExecutionReport, timestamp: Instant = Instant.now())
