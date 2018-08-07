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

  override def projectId: String = "sparqlSelectProject"

  override def singleWorkspaceProviderId: String = "inMemory"

  private val workflow = "sparqlSelectWorkflow"

  behavior of "SPARQL select task"

  it should "produce the correct result" in {
    checkOutputResource("sparqlOutput.csv", "s,v")
    implicit val userContext: UserContext = UserContext.Empty
    val workflowTask = project.task[Workflow](workflow)
    val executeActivity = workflowTask.activity[LocalWorkflowExecutorGeneratingProvenance]
    executeActivity.control.startBlocking()
    val expectedResult = """s,v
      |http://ns.eccenca.com/unemployment4,6.1
      |http://ns.eccenca.com/unemployment1,6.2
      |http://ns.eccenca.com/unemployment15,6.8
      |http://ns.eccenca.com/unemployment12,6.3
      |http://ns.eccenca.com/unemployment8,6""".stripMargin
    checkOutputResource("sparqlOutput.csv", expectedResult)
  }

  private def checkOutputResource(name: String, expectedResult: String): Unit = {
    val outputResource = project.resources.getInPath(name)
    outputResource.loadAsString("UTF-8") mustBe expectedResult
  }
}
