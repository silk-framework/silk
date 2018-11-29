package org.silkframework.execution.local

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutor, LocalWorkflowExecutorGeneratingProvenance, Workflow}

/**
  * Tests the SPARQL select task in a workflow.
  */
class SparqlSelectIntegrationTest extends FlatSpec with SingleProjectWorkspaceProviderTestTrait with MustMatchers {
  implicit val userContext: UserContext = UserContext.Empty
  override def projectPathInClasspath: String = "org/silkframework/execution/SPARQLselect.zip"

  override def projectId: String = "sparqlSelectProject"

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
      |urn:instance:unemploymentcsv#17,6.9
      |urn:instance:unemploymentcsv#6,5.8
      |urn:instance:unemploymentcsv#10,6.1
      |urn:instance:unemploymentcsv#2,6.2""".stripMargin
    checkOutputResource("sparqlOutput.csv", expectedResult)
  }

  private def checkOutputResource(name: String, expectedResult: String): Unit = {
    val outputResource = project.resources.getInPath(name)
    outputResource.loadAsString("UTF-8") mustBe expectedResult
  }
}
