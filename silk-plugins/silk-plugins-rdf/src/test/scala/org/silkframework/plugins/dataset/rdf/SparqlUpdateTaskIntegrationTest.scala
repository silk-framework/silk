package org.silkframework.plugins.dataset.rdf

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow}

/**
  * Execute a workflow using the SPARQL Update task and check the result.
  */
class SparqlUpdateTaskIntegrationTest extends FlatSpec with MustMatchers with SingleProjectWorkspaceProviderTestTrait {
  behavior of "SPARQL Update Task in a Workflow"

  override def workspaceProviderId: String = "inMemory"

  it should "generate the correct result" in {
    project.task[Workflow]("workflow").activity[LocalWorkflowExecutorGeneratingProvenance].control.startBlocking()
    /*
    Workflow description:
    1. Transform CSV to RDF (in-memory)
    2. run Sparql Select on RDF (in-memory)
    3. feed SPARQL select result into SPARQL Update that generates new properties
    4. Transform RDF to CSV using the new properties
     */
    project.resources.getInPath("output.csv").loadLines mustBe Seq(
      "generated", "Abbigail Lesch", "Abbigail Ziemann", "Abigale Purdy", "Ronny Wiegand", "Rosalia Lueilwitz",
      "Rosalyn Wisozk", "Rosamond Rath", "Willy Rath"
    )
  }

  override def projectPathInClasspath: String = "org/silkframework/plugins/dataset/rdf/sparqlUpdateProject.zip"

}
