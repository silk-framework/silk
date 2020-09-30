package org.silkframework.workspace.activity.workflow

import org.silkframework.execution.ExecutionReport
import org.silkframework.util.Identifier

case class WorkflowExecutionReport(label: String, taskReports: Map[Identifier, ExecutionReport] = Map.empty) extends ExecutionReport {

  def withReport(taskId: String, executionReport: ExecutionReport): WorkflowExecutionReport = {
    copy(taskReports = taskReports + ((taskId, executionReport)))
  }

  override def summary: Seq[(String, String)] = Seq.empty

  override def warnings: Seq[String] = {
    if(taskReports.values.exists(_.warnings.nonEmpty)) {
      Seq("Some tasks generated warnings.")
    } else {
      Seq.empty
    }
  }
}
