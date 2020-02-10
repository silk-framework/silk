package org.silkframework.plugins.dataset.rdf

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow}

class WorkflowReconfigurationIntegrationTest extends FlatSpec with MustMatchers with SingleProjectWorkspaceProviderTestTrait {
  behavior of "Workflow reconfiguration"

  override def projectPathInClasspath: String = "diProjects/configProject.zip"

  /*
    The workflow reads person entities from a CSV and transforms them to RDF.
    A SPARQL Select operator reads from the RDF dataset and writes into an output CSV dataset.
    Both the SPARQL Select operator and the output CSV dataset are re-configured:
    - The SPARQL Select operator with a limit of 1 (The transformed RDF data has 2 entities)
    - The CSV output dataset with a '|' separator
   */
  private val workflowId = "reconfiguredWorkflow"
  private val outputFile = "sparqlSelect.csv"

  override def workspaceProviderId: String = "inMemory"

  it should "re-configure tasks in a workflow execution" in {
    executeWorkflow(workflowId)
    project.resources.get(outputFile).loadAsString.split("[\r\n]+") mustBe Seq(
      "name|id",
      "Max Noe|2"
    )
  }
}
