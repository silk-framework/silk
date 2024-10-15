package controllers.workspaceApi.search

import controllers.workspaceApi.search.SearchApiModel.Facets
import org.silkframework.runtime.activity.{Status, UserContext}
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow}

/**
  * Facet collector for workflows.
  */
case class WorkflowFacetCollector() extends ItemTypeFacetCollector[ProjectTask[Workflow]] {
  override val facetCollectors: Seq[FacetCollector[ProjectTask[Workflow]]] = {
    Seq(
      WorkflowExecutionStatus(),
      WorkflowReplaceableInputOutput()
    )
  }
}

/** Facet to filter a workflow by its status. */
case class WorkflowExecutionStatus() extends NoLabelKeywordFacetCollector[ProjectTask[Workflow]] {

  override def extractKeywordIds(projectTask: ProjectTask[Workflow])
                                (implicit user: UserContext): Set[String] = {
    val executionActivity = projectTask.activity[LocalWorkflowExecutorGeneratingProvenance]
    executionActivity.status.get.toSet map { status: Status =>
      status.concreteStatus
    }
  }

  override def appliesForFacet: SearchApiModel.Facet = Facets.workflowExecutionStatus
}

/** Facet to filter a workflow by it including replaceable input/output datasets. */
case class WorkflowReplaceableInputOutput() extends NoLabelKeywordFacetCollector[ProjectTask[Workflow]] {

  override def extractKeywordIds(projectTask: ProjectTask[Workflow])
                                (implicit user: UserContext): Set[String] = {
    var keywords = Set.empty[String]
    val workflow = projectTask.data
    val variableDatasets = workflow.legacyVariableDatasets(projectTask.project)
    if(workflow.replaceableInputs.nonEmpty || variableDatasets.dataSources.nonEmpty) {
      keywords = keywords + "Input"
    }
    if(workflow.replaceableOutputs.nonEmpty || variableDatasets.dataSources.nonEmpty) {
      keywords = keywords + "Output"
    }
    keywords
  }

  override def appliesForFacet: SearchApiModel.Facet = Facets.workflowInputOutput
}
