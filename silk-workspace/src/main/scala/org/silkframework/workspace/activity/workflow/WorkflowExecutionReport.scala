package org.silkframework.workspace.activity.workflow

import org.silkframework.execution.ExecutionReport
import org.silkframework.util.Identifier

case class WorkflowExecutionReport(taskReports: Map[Identifier, ExecutionReport] = Map.empty) extends ExecutionReport {

  def withReport(taskId: String, executionReport: ExecutionReport): WorkflowExecutionReport = {
    copy(taskReports = taskReports + ((taskId, executionReport)))
  }

}
