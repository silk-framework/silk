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
case class WorkflowExecutionReport(task: Task[TaskSpec], taskReports: Map[Identifier, ExecutionReport] = Map.empty) extends ExecutionReport {

  def withReport(nodeId: String, executionReport: ExecutionReport): WorkflowExecutionReport = {
    copy(taskReports = taskReports + ((nodeId, executionReport)))
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
