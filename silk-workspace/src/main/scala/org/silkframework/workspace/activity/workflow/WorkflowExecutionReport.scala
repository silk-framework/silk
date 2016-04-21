package org.silkframework.workspace.activity.workflow

import org.silkframework.execution.ExecutionReport

import scala.collection.immutable.ListMap

case class WorkflowExecutionReport(taskReports: ListMap[String, ExecutionReport] = ListMap.empty) extends ExecutionReport {

  def withReport(taskId: String, executionReport: ExecutionReport): WorkflowExecutionReport = {
    copy(taskReports = taskReports + ((taskId, executionReport)))
  }

}
