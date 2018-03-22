package org.silkframework.workspace.activity.workflow

import org.silkframework.dataset.Dataset
import org.silkframework.workspace.ProjectTask

/**
  * Executes a workflow with the local workflow executor, generates provenance data (PROV-O) and writes it into
  * the backend.
  */
case class LocalWorkflowExecutorGeneratingProvenance(workflowTask: ProjectTask[Workflow],
                                                     replaceDataSources: Map[String, Dataset] = Map.empty,
                                                     replaceSinks: Map[String, Dataset] = Map.empty,
                                                     useLocalInternalDatasets: Boolean = false) extends WorkflowExecutorGeneratingProvenance {
  override def workflowExecutionActivity(): LocalWorkflowExecutor = LocalWorkflowExecutor(workflowTask, replaceDataSources, replaceSinks, useLocalInternalDatasets)
}