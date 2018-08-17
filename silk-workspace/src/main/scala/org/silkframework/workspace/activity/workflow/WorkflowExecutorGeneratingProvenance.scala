package org.silkframework.workspace.activity.workflow

import java.util.logging.Logger

import org.silkframework.runtime.activity._
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.workspace.ProjectTask

/**
  * Executes a workflow child activity and generates provenance data (PROV-O) and writes it into the backend.
  */
trait WorkflowExecutorGeneratingProvenance extends Activity[WorkflowExecutionReportWithProvenance] {
  def workflowTask: ProjectTask[Workflow]
  private val log = Logger.getLogger(getClass.getName)

  /** The activity that executes the workflow and produces a workflow execution report */
  def workflowExecutionActivity(): Activity[WorkflowExecutionReport]

  override def run(context: ActivityContext[WorkflowExecutionReportWithProvenance]): Unit = {
    val workflowExecutor: Activity[WorkflowExecutionReport] = workflowExecutionActivity()
    implicit val userContext: UserContext = context.userContext
    val control = context.child(workflowExecutor, 0.95)
    try {
      log.fine("Start child workflow executor activity")
      control.start()
      control.waitUntilFinished()
    } finally {
      control.lastResult match {
        case Some(lastResult) =>
          val report = WorkflowExecutionReportWithProvenance.fromActivityExecutionReport(lastResult)
          context.value.update(report)
          val persistProvenanceService = PluginRegistry.createFromConfig[PersistWorkflowProvenance]("provenance.persistWorkflowProvenancePlugin")
          persistProvenanceService.persistWorkflowProvenance(workflowTask, lastResult)
        case None =>
          throw new RuntimeException("Child activity 'Execute local workflow' did not finish with result!")
      }
    }
  }
}

case class WorkflowExecutionReportWithProvenance(report: WorkflowExecutionReport, workflowExecutionProvenance: WorkflowExecutionProvenanceData)

object WorkflowExecutionReportWithProvenance {
  def fromActivityExecutionReport(activityResult: ActivityExecutionResult[WorkflowExecutionReport]): WorkflowExecutionReportWithProvenance = {
    val workflowExecutionProvenance = WorkflowExecutionProvenanceData(activityResult.metaData)
    WorkflowExecutionReportWithProvenance(activityResult.resultValue.get, workflowExecutionProvenance)
  }

  val empty = WorkflowExecutionReportWithProvenance(
    report = WorkflowExecutionReport(),
    workflowExecutionProvenance = WorkflowExecutionProvenanceData(ActivityExecutionMetaData())
  )
}

case class WorkflowExecutionProvenanceData(activityMetaData: ActivityExecutionMetaData)