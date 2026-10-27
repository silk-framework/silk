package org.silkframework.workspace.activity.workflow


import org.silkframework.dataset.{MockDataset, MockDatasetExecutor}
import org.silkframework.rule.execution.TransformReport
import org.silkframework.runtime.activity.{ActivityMonitor, UserContext}
import org.silkframework.runtime.plugin.{PluginContext, PluginRegistry, TestPluginContext}
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.changes.{ChangeConflictException, WorkflowExecuted}
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
    val availableReports = executionMgr.listReports(projectIds = Set(projectId))
    availableReports should have size 1

    // The activity value carries the identifier of the persisted report
    val reportId = workflow.activity[LocalWorkflowExecutorGeneratingProvenance].value().reportId
    reportId shouldBe Some(availableReports.head)
    executionMgr.retrieveReport(reportId.get).resultValue shouldBe defined

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

  it should "record the run in the change journal, but not the files it wrote" in {
    val journal = project.changeJournal
    val recorded = journal.all.size
    val workflow = project.task[Workflow]("workflow")
    workflow.activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()

    val entries = journal.all.drop(recorded)
    entries.map(_.change.describe) shouldBe Seq("Executed workflow 'workflow'")
    val executed = entries.head.change.asInstanceOf[WorkflowExecuted]
    executed.executionId shouldBe workflow.activity[LocalWorkflowExecutorGeneratingProvenance].value().reportId.map(_.time.toString)
    executed.failed shouldBe false
    a[ChangeConflictException] should be thrownBy journal.revert(entries.head.seq)
  }

  it should "end the run if the clearing of an output dataset fails" in {
    PluginRegistry.unregisterPlugin(classOf[MockDatasetExecutor])
    PluginRegistry.registerPlugin(classOf[MockDatasetExecutor])
    val workflow = project.task[Workflow]("workflow")
    val outputDataset = MockDataset()
    outputDataset.clearFn = () => throw new IllegalStateException("Clearing failed")
    val context = new ActivityMonitor[WorkflowExecutionReport]("workflowRun", initialValue = Some(WorkflowExecutionReport(workflow)))

    val error = intercept[WorkflowExecutionException] {
      LocalWorkflowExecutor(workflow, replaceSinks = Map("output" -> outputDataset)).run(context)(UserContext.Empty)
    }

    error.getMessage shouldBe "Failed to clear dataset 'output': Clearing failed"
    error.getCause shouldBe a[IllegalStateException]
    context.value().taskReports shouldBe empty // No node has been executed
    context.value().error shouldBe Some(error.getMessage)
    context.value().isDone shouldBe true // The run has ended, although the clearing failed
  }

  override def propertyMap: Map[String, Option[String]] = {
    Map(
      "workspace.reportManager.plugin" -> Some("inMemoryExecutionReportManager"),
      // No store is configured by default, which records nothing.
      "workspace.changes.plugin" -> Some("inMemoryChangeJournal")
    )
  }
}
