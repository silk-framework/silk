package org.silkframework.execution.local


import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

/**
  * Tests the SPARQL select task in a workflow.
  */
class SparqlSelectIntegrationTest extends AnyFlatSpec with SingleProjectWorkspaceProviderTestTrait with Matchers {
  override def projectPathInClasspath: String = "org/silkframework/execution/SPARQLselect.zip"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  private val workflow = "sparqlSelectWorkflow"

  behavior of "SPARQL select task"

  it should "produce the correct result" in {
    checkOutputResource("sparqlOutput.csv", "s,v")
    val workflowTask = project.task[Workflow](workflow)
    val executeActivity = workflowTask.activity[LocalWorkflowExecutorGeneratingProvenance]
    executeActivity.control.startBlocking()

    val expectedResult = """s,v
       |urn:instance:unemploymentcsv#1,6
       |urn:instance:unemploymentcsv#10,6.1
       |urn:instance:unemploymentcsv#11,6.1
       |urn:instance:unemploymentcsv#12,6.2
       |urn:instance:unemploymentcsv#13,6.3""".stripMargin
    checkOutputResource("sparqlOutput.csv", expectedResult)
  }

  private def checkOutputResource(name: String, expectedResult: String): Unit = {
    val outputResource = project.resources.getInPath(name)
    val loaded = outputResource.loadLines().sorted
    loaded.mkString("") mustBe expectedResult.replaceAll("\\s+", "")
  }
}
