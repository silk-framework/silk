package org.silkframework.execution.local

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutor, LocalWorkflowExecutorGeneratingProvenance, Workflow}

/**
  * Tests the SPARQL select task in a workflow.
  */
class SparqlSelectIntegrationTest extends FlatSpec with SingleProjectWorkspaceProviderTestTrait with MustMatchers {
  override def projectPathInClasspath: String = "org/silkframework/execution/SPARQLselect.zip"

  override def workspaceProvider: String = "inMemory"

  private val workflow = "sparqlSelectWorkflow"

  behavior of "SPARQL select task"

  it should "produce the correct result" in {
    checkOutputResource("sparqlOutput.csv", "s,v")
    val workflowTask = project.task[Workflow](workflow)
    val executeActivity = workflowTask.activity[LocalWorkflowExecutorGeneratingProvenance]
    executeActivity.control.startBlocking()

    val expectedResult = """s,v
       |urn:instance:unemploymentcsv#14,6.5
       |urn:instance:unemploymentcsv#18,7.1
       |urn:instance:unemploymentcsv#5,6.1
       |urn:instance:unemploymentcsv#6,5.8
       |urn:instance:unemploymentcsv#8,5.8""".stripMargin
    checkOutputResource("sparqlOutput.csv", expectedResult)
  }

  private def checkOutputResource(name: String, expectedResult: String): Unit = {
    val outputResource = project.resources.getInPath(name)
    val loaded = outputResource.loadLines.sorted
    loaded.mkString("") mustBe expectedResult.replaceAll("\\s+", "")
  }
}
