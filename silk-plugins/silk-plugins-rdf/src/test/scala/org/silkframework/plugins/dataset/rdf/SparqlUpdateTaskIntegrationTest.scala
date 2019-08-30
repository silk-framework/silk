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

  private val identity: String => String = (input: String) => input
  private val taskPropertyConcatenate = (input: String) => s"20 $input true"

  for((templatingMode, workflowId, outputResourceName, resultFn) <- Seq(
    // Uses simple templating mode
    ("simple", "workflow", "output.csv", identity),
    // Uses Velocity templating mode
    ("Velocity", "workflowVelocity", "outputVelocity.csv", identity),
    // Uses Velocity templating mode and accessed input and output task properties
    ("Velocity with task properties", "workflowVelocityTaskProperties", "outputVelocity.csv", taskPropertyConcatenate) //
  )) {
    it should s"generate the correct result in '$templatingMode' templating mode" in {
      project.task[Workflow](workflowId).activity[LocalWorkflowExecutorGeneratingProvenance].control.startBlocking()
      /*
      Workflow description:
      1. Transform CSV to RDF (in-memory)
      2. run Sparql Select on RDF (in-memory)
      3. feed SPARQL select result into SPARQL Update that generates new properties
      4. Transform RDF to CSV using the new properties
       */
      project.resources.getInPath(outputResourceName).loadLines mustBe Seq("generated") ++ // CSV Header
          Seq("Abbigail Lesch", "Abbigail Ziemann", "Abigale Purdy", "Ronny Wiegand", "Rosalia Lueilwitz",
            "Rosalyn Wisozk", "Rosamond Rath", "Willy Rath"
          ).map(resultFn)
    }
  }

  override def projectPathInClasspath: String = "org/silkframework/plugins/dataset/rdf/sparqlUpdateProject.zip"

}
