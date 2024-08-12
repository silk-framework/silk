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
case class WorkflowExecutionReport(task: Task[TaskSpec], taskReports: IndexedSeq[WorkflowTaskReport] = IndexedSeq.empty,
                                   isDone: Boolean = false, version: Int = 0) extends ExecutionReport {

  /**
    * Retrieves all current task reports.
    * If there are multiple reports for a single node, only the most recent one is returned.
    */
  def currentReports(): Iterable[WorkflowTaskReport] = {
    taskReports.groupBy(_.nodeId).values.map(_.maxBy(_.timestamp))
  }

  /**
    * Retrieves all task reports for a given workflow node id.
    * Will be empty if there are no reports found.
    */
  def retrieveReports(nodeId: Identifier): Seq[ExecutionReport] = {
    taskReports.filter(_.nodeId == nodeId).map(_.report)
  }

  /**
    * Adds a new task report.
    *
    * @return The updated workflow report
    */
  def addReport(nodeId: Identifier, report: ExecutionReport): WorkflowExecutionReport = {
    copy(taskReports = taskReports :+ WorkflowTaskReport(nodeId, report, version), version = version + 1)
  }

  /**
    * Updates an existing task report.
    *
    * @return The updated workflow report
    */
  def updateReport(index: Int, nodeId: Identifier, report: ExecutionReport): WorkflowExecutionReport = {
    if(index < taskReports.size) {
      copy(taskReports = taskReports.updated(index, WorkflowTaskReport(nodeId, report, version + 1, Instant.now())), version = version + 1)
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
    taskReports.zipWithIndex.findLast(_._1.nodeId == nodeId) match {
      case Some((workflowReport, index)) =>
        val timestamp = Instant.now()
        val report = workflowReport.report
        val errorMsg = ex.getMessage
        val errorReport = SimpleExecutionReport(report.task, report.summary, report.warnings, Some(errorMsg), isDone = true, report.entityCount, report.operation, report.operationDesc)
        copy(taskReports = taskReports.updated(index, WorkflowTaskReport(nodeId, errorReport, version + 1, timestamp)), version = version + 1)
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
    val timestamp = Instant.now()
    // Marks a report as done. Only updates reports that are not already done to avoid unnecessary updates.
    def updateReport(taskReport: WorkflowTaskReport): WorkflowTaskReport = {
      if(taskReport.report.isDone) {
        taskReport
      } else {
        taskReport.copy(report = taskReport.report.asDone(), version = version + 1, timestamp = timestamp)
      }
    }
    copy(taskReports = taskReports.map(updateReport), isDone = true, version = version + 1)
  }

  override def summary: Seq[(String, String)] = Seq.empty

  override def warnings: Seq[String] = {
    if(taskReports.exists(_.report.warnings.nonEmpty)) {
      Seq("Some tasks generated warnings.")
    } else {
      Seq.empty
    }
  }

  override def entityCount: Int = taskReports.map(_.nodeId).distinct.size

  override def operationDesc: String = "nodes executed"
}

/**
  * Report of a single workflow operator execution.
  *
  * @param nodeId The node identifier within the workflow
  * @param report The execution report.
  * @param timestamp Timestamp of the last update.
  */
case class WorkflowTaskReport(nodeId: Identifier, report: ExecutionReport, version: Int = 0, timestamp: Instant = Instant.now())
