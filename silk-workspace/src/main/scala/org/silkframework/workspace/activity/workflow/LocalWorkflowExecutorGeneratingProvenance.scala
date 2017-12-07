package org.silkframework.workspace.activity.workflow

import java.util.logging.Logger

import org.silkframework.dataset.Dataset
import org.silkframework.runtime.activity._
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.workspace.ProjectTask

/**
  * Executes a workflow with the local workflow executor, generates provenance data (PROV-O) and writes it into
  * the backend.
  */
case class LocalWorkflowExecutorGeneratingProvenance(workflowTask: ProjectTask[Workflow],
                                                     replaceDataSources: Map[String, Dataset] = Map.empty,
                                                     replaceSinks: Map[String, Dataset] = Map.empty,
                                                     useLocalInternalDatasets: Boolean = false) extends Activity[WorkflowExecutionReportWithProvenance] {
  private val log = Logger.getLogger(getClass.getName)

  override def run(context: ActivityContext[WorkflowExecutionReportWithProvenance]): Unit = {
    val localWorkflowExecutor = LocalWorkflowExecutor(workflowTask, replaceDataSources, replaceSinks, useLocalInternalDatasets)
    implicit val userContext: UserContext = context.userContext
    val control = context.child(localWorkflowExecutor, 0.8)
    control.start()
    control.waitUntilFinished()
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