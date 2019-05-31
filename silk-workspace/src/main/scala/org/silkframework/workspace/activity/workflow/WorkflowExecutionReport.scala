package org.silkframework.workspace.activity.workflow

import org.silkframework.execution.ExecutionReport
import org.silkframework.util.Identifier

case class WorkflowExecutionReport(label: String, taskReports: Map[Identifier, ExecutionReport] = Map.empty) extends ExecutionReport {

  def withReport(taskId: String, executionReport: ExecutionReport): WorkflowExecutionReport = {
    copy(taskReports = taskReports + ((taskId, executionReport)))
  }

  override def summary: Seq[(String, String)] = Seq.empty

  override def warning: Option[String] = {
    if(taskReports.values.exists(_.warning.nonEmpty)) {
      Some("Some tasks generated warnings.")
    } else {
      None
    }
  }
}
