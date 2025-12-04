package org.silkframework.plugins.dataset.rdf


import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow}

/**
  * Execute a workflow using the SPARQL Update task and check the result.
  */
class SparqlUpdateTaskIntegrationTest extends AnyFlatSpec with Matchers with SingleProjectWorkspaceProviderTestTrait {
  behavior of "SPARQL Update Task in a Workflow"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

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
      executeWorkflow(workflowId)
      /*
      Workflow description:
      1. Transform CSV to RDF (in-memory)
      2. run Sparql Select on RDF (in-memory)
      3. feed SPARQL select result into SPARQL Update that generates new properties
      4. Transform RDF to CSV using the new properties
       */
      project.resources.getInPath(outputResourceName).loadLines() mustBe Seq("generated") ++ // CSV Header
          Seq("Abbigail Lesch", "Abbigail Ziemann", "Abigale Purdy", "Ronny Wiegand", "Rosalia Lueilwitz",
            "Rosalyn Wisozk", "Rosamond Rath", "Willy Rath"
          ).map(resultFn)
      val workflowReport = project.task[Workflow](workflowId).activity[LocalWorkflowExecutorGeneratingProvenance].value.get
      workflowReport mustBe defined
      val taskReports = workflowReport.get.report.taskReports
      // 8 operators, the 2 intermediate datasets have 2 reports each
      taskReports must have size 10
      taskReports.map(r => r.report.summary.filter(item => item._1.startsWith("Number of executions")).map(_._2).mkString) mustBe Seq(
        // Transformations and datasets read by the SPARQL select task do not have a 'Number of executions' summary item (yet)
        "1", "", "1", "", "1", "1", "1", "1", "", "1"
      )
      val sparqlSelectTaskReport = taskReports(4).report
      sparqlSelectTaskReport.entityCount mustBe 8
      sparqlSelectTaskReport.summary.filter(r => r._1 == "No. of rows processed").map(_._2) mustBe Seq("8")

      val sparqlUpdateTaskReport = taskReports(5).report
      // Batch size is set to 2, so half the number of the SPARQL Select task
      sparqlUpdateTaskReport.entityCount mustBe 4
      sparqlUpdateTaskReport.summary.filter(r => r._1 == "No. of queries generated").map(_._2) mustBe Seq("4")
    }
  }

  override def projectPathInClasspath: String = "org/silkframework/plugins/dataset/rdf/sparqlUpdateProject.zip"

}
