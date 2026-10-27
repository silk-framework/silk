package org.silkframework.workspace.activity.workflow

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{PlainTask, Task, TaskSpec}
import org.silkframework.execution.{ExecutionReport, ExecutionReportUpdater}
import org.silkframework.runtime.activity.Status.Canceling
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor, UserContext}
import org.silkframework.util.Identifier

/**
  * Tests that task reports are correctly added to the workflow execution report.
  */
class WorkflowRunContextTest extends AnyFlatSpec with Matchers {

  behavior of "WorkflowRunContext"

  private val workflowTask = PlainTask("workflow", WorkflowTest.testWorkflow)
  private val datasetTask: Task[TaskSpec] = PlainTask("dataset1", WorkflowTest.testWorkflow)

  it should "add a separate report entry for each execution on the same task context" in {
    val monitor = new ActivityMonitor[WorkflowExecutionReport]("workflowRun", initialValue = Some(WorkflowExecutionReport(workflowTask)))
    implicit val runContext: WorkflowRunContext = WorkflowRunContext(monitor, WorkflowTest.testWorkflow, UserContext.Empty)
    val nodeId = Identifier("dataset1")
    val taskContext = runContext.taskContext(nodeId, datasetTask)

    // The initial report entry is added when the task context is created
    monitor.value().taskReports.map(_.nodeId) shouldBe IndexedSeq(nodeId)

    // First execution, e.g. a clear instruction written to a dataset
    val clearUpdater = reportUpdater(taskContext, singular = "clear instruction", plural = "clear instructions")
    clearUpdater.increaseEntityCounter()
    clearUpdater.executionDone()
    monitor.value().taskReports should have size 1

    // Second execution on the same context, e.g. SPARQL update queries written to the same dataset
    val queryUpdater = reportUpdater(taskContext, singular = "update query", plural = "update queries")
    queryUpdater.increaseEntityCounter()
    queryUpdater.executionDone()

    val reports = monitor.value().taskReports
    reports.map(_.nodeId) shouldBe IndexedSeq(nodeId, nodeId)
    // Each execution reports its own entity count, i.e. counts are not summed up across executions
    reports.map(_.report.entityCount) shouldBe IndexedSeq(1, 1)
    reports.map(_.report.isDone) shouldBe IndexedSeq(true, true)
    reports.head.report.operationDesc should include ("clear instruction")
    reports.last.report.operationDesc should include ("update query")
  }

  it should "record the tasks that are still running when the workflow is cancelled" in {
    val monitor = new ActivityMonitor[WorkflowExecutionReport]("workflowRun", initialValue = Some(WorkflowExecutionReport(workflowTask)))
    val runContext = WorkflowRunContext(monitor, WorkflowTest.testWorkflow, UserContext.Empty)
    runContext.trackOperation("Executing 'plugin'") {
      runContext.trackOperation("Writing 'output'") {
        runContext.trackOperation("Reading 'input'") {} // The input has been read completely
        runContext.trackOperation("Executing 'plugin'") { // The plugin is executed a second time, e.g. for its error output
          monitor.status.update(Canceling(None))
        }
      }
    }

    monitor.value().workflowWarnings shouldBe Seq("The workflow was cancelled while the following tasks were still running: " +
      "Executing 'plugin', Writing 'output'. The workflow stops once they have finished.")
    monitor.value().version shouldBe 1 // Report consumers rely on a version bump for every change

    runContext.runFinished() // The tasks have finished, so the warning gets its final wording

    monitor.value().workflowWarnings should have size 1
    monitor.value().workflowWarnings.head should fullyMatch regex ("The workflow was cancelled while the following tasks were still running: " +
      "Executing 'plugin', Writing 'output'\\. They finished after (\\d+ms|\\d+\\.\\d+s)\\.")
    monitor.value().version shouldBe 2
  }

  private def reportUpdater(taskContext: ActivityContext[ExecutionReport], singular: String, plural: String): ExecutionReportUpdater = {
    new ExecutionReportUpdater {
      override def task: Task[TaskSpec] = datasetTask
      override def context: ActivityContext[ExecutionReport] = taskContext
      override def entityLabelSingle: String = singular
      override def entityLabelPlural: String = plural
      override def entityProcessVerb: String = "executed"
      override def minEntitiesBetweenUpdates: Int = 1
    }
  }
}
