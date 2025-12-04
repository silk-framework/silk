package org.silkframework.workspace.activity.workflow


import org.silkframework.rule.execution.TransformReport
import org.silkframework.runtime.plugin.{PluginContext, TestPluginContext}
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.reports.ExecutionReportManager
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Tests if the workflow report is generated and errors are written to the error output.
  */
class LocalWorkflowExecutorTest extends AnyFlatSpec with Matchers with SingleProjectWorkspaceProviderTestTrait with ConfigTestTrait {

  private implicit val pluginContext: PluginContext = TestPluginContext(resources = InMemoryResourceManager())

  override def projectPathInClasspath: String = "org/silkframework/workspace/activity/workflow/executionReportTest.zip"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  override def projectId: String = "executionReportTest"

  behavior of "LocalWorkflowExecutor"

  it should "generate a workflow execution report and write errors to the error output." in {
    val executionMgr = ExecutionReportManager()

    // Execute workflow
    val workflow = project.task[Workflow]("workflow")
    workflow.activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()

    // Test if a report has been written
    val availableReports = executionMgr.listReports(projectId = Some(projectId))
    availableReports should have size 1

    // Retrieve transform report
    val lastReport = executionMgr.retrieveReport(availableReports.last)
    val workflowReport = lastReport.resultValue.get.asInstanceOf[WorkflowExecutionReport]
    val transformReport = workflowReport.taskReports.find(_.nodeId.toString == "transform").get.report.asInstanceOf[TransformReport]

    // Check if expected errors have been recorded
    transformReport.ruleResults("uri").errorCount shouldBe 1
    transformReport.ruleResults("name").errorCount shouldBe 0
    transformReport.ruleResults("age").errorCount shouldBe 1
    transformReport.ruleResults("city").errorCount shouldBe 0

    // Check if output has been written correctly
    project.resources.get("output.csv").loadLines() shouldBe
      Seq(
        "name,age,city",
        "Max Mustermann,40,Berlin",
        "Max Weber,,Leipzig"
      )

    // Check if error output has been written correctly
    project.resources.get("errorOutput.csv").loadLines() shouldBe
      Seq(
        "name,age,city,error",
        "Max Weber,,Leipzig,Value 'unknown' is not a valid Int"
      )
  }

  override def propertyMap: Map[String, Option[String]] = {
    Map(
      "workspace.reportManager.plugin" -> Some("inMemoryExecutionReportManager")
    )
  }
}
