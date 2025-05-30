package org.silkframework.workspace.activity.workflow

import org.silkframework.config.PlainTask
import org.silkframework.runtime.activity._
import org.silkframework.runtime.plugin.{PluginContext, PluginRegistry}
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.reports.{ExecutionReportManager, ReportIdentifier}

import java.util.logging.Logger

/**
  * Executes a workflow child activity and generates provenance data (PROV-O) and writes it into the backend.
  */
trait WorkflowExecutorGeneratingProvenance extends Activity[WorkflowExecutionReportWithProvenance] {

  def workflowTask: ProjectTask[Workflow]

  private val log = Logger.getLogger(getClass.getName)

  override def initialValue: Option[WorkflowExecutionReportWithProvenance] = {
    Some(
      WorkflowExecutionReportWithProvenance(
        report = WorkflowExecutionReport(workflowTask),
        workflowExecutionProvenance = WorkflowExecutionProvenanceData(ActivityExecutionMetaData())
    ))
  }

  /** The activity that executes the workflow and produces a workflow execution report */
  def workflowExecutionActivity(): Activity[WorkflowExecutionReport]

  override def run(context: ActivityContext[WorkflowExecutionReportWithProvenance])
                  (implicit userContext: UserContext): Unit = {
    implicit val pluginContext: PluginContext = PluginContext.fromProject(workflowTask.project)
    val workflowExecutor: Activity[WorkflowExecutionReport] = workflowExecutionActivity()
    val control = context.child(workflowExecutor, 1.0)
    var executionException: Option[Throwable] = None
    try {
      log.fine("Start child workflow executor activity")
      // Propagate workflow execution report
      val listener = (executionReport: WorkflowExecutionReport) => {
        context.value.update(WorkflowExecutionReportWithProvenance(executionReport, WorkflowExecutionProvenanceData(ActivityExecutionMetaData())))
      }
      control.value.subscribe(listener)
      control.startBlocking()
    } catch {
      case ex: Throwable =>
        executionException = Some(ex)
        throw ex
    } finally {
      try {
        control.lastResult match {
          case Some(lastResult) =>
            val report = WorkflowExecutionReportWithProvenance.fromActivityExecutionReport(lastResult)
            context.value.update(report)
            ExecutionReportManager().addReport(ReportIdentifier.create(workflowTask.project.id, workflowTask.id), lastResult)
            val persistProvenanceService = PluginRegistry.createFromConfig[PersistWorkflowProvenance]("provenance.persistWorkflowProvenancePlugin")
            persistProvenanceService.persistWorkflowProvenance(workflowTask, lastResult)
          case None =>
            throw new RuntimeException("Child activity 'Execute local workflow' did not finish with result!")
        }
      } catch {
        case ex: Throwable =>
          // If the execution failed, we want to throw the original execution exception
          executionException match {
            case Some(originalException) =>
              originalException.addSuppressed(ex)
              throw originalException
            case None =>
              throw ex
          }
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
    report = WorkflowExecutionReport(PlainTask("emptyReport", Workflow())),
    workflowExecutionProvenance = WorkflowExecutionProvenanceData(ActivityExecutionMetaData())
  )
}

case class WorkflowExecutionProvenanceData(activityMetaData: ActivityExecutionMetaData)