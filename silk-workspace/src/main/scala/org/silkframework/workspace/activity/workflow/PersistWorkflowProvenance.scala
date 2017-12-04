package org.silkframework.workspace.activity.workflow

import java.util.logging.Logger

import org.silkframework.runtime.activity.ActivityExecutionResult
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.ProjectTask

/**
  * Persists provenance data about a workflow execution, e.g. in an RDF store.
  */
trait PersistWorkflowProvenance {
  /**
    * Persists provenance data about a workflow execution, e.g. in an RDF store.
    * @param workflowTask   The executed workflow task
    * @param activityResult The result of executing the workflow
    */
  def persistWorkflowProvenance(workflowTask: ProjectTask[Workflow],
                                activityResult: ActivityExecutionResult[WorkflowExecutionReport]): Unit
}

@Plugin(
  id = "nopWorkflowProvenance",
  label = "NOP Workflow Provenance Plugin",
  description = "Placeholder workflow provenance plugin. Outputs warnings in log that no plugin is installed when trying to write provenance data."
)
case class NopPersistWorkflowProvenance() extends PersistWorkflowProvenance {
  val log: Logger = Logger.getLogger(this.getClass.getName)
  override def persistWorkflowProvenance(workflowTask: ProjectTask[Workflow],
                                         activityResult: ActivityExecutionResult[WorkflowExecutionReport]): Unit = {
    log.fine("Workflow provenance data ist not written. No valid plugin specified. Please set provenance.persistWorkflowProvenancePlugin")
  }
}