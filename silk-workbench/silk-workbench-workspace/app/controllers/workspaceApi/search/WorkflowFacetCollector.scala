package controllers.workspaceApi.search

import controllers.workspaceApi.search.SearchApiModel.Facets
import org.silkframework.runtime.activity.Status
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow}

/**
  * Facet collector for workflows.
  */
case class WorkflowFacetCollector() extends ItemTypeFacetCollector[Workflow] {
  override val facetCollectors: Seq[FacetCollector[Workflow]] = {
    Seq(
      WorkflowExecutionStatus()
    )
  }
}

/** Facet to filter a workflow by its status. */
case class WorkflowExecutionStatus() extends NoLabelKeyboardFacetCollector[Workflow] {

  override def extractKeywordIds(projectTask: ProjectTask[Workflow]): Set[String] = {
    val executionActivity = projectTask.activity[LocalWorkflowExecutorGeneratingProvenance]
    executionActivity.status.get.toSet map { status: Status =>
      if(status.failed) {
        "Fail"
      } else if(status.succeeded) {
        "Success"
      } else if(status.isRunning) {
        "Running"
      } else {
        "Not executed"
      }
    }
  }

  override def appliesForFacet: SearchApiModel.Facet = Facets.workflowExecutionStatus
}
