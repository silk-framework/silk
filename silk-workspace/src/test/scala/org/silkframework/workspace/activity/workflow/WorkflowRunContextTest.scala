package org.silkframework.workspace.activity.workflow

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{PlainTask, Task, TaskSpec}
import org.silkframework.execution.{ExecutionReport, ExecutionReportUpdater}
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.TransformReport
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor, UserContext}
import org.silkframework.util.Identifier

/**
  * Tests that task reports are correctly added to the workflow execution report.
  */
class WorkflowRunContextTest extends AnyFlatSpec with Matchers {

  behavior of "WorkflowRunContext"

  private val workflowTask = PlainTask("workflow", WorkflowTest.testWorkflow)
  private val datasetTask: Task[TaskSpec] = PlainTask("dataset1", WorkflowTest.testWorkflow)
  private val transformTask = PlainTask("transform", TransformSpec.empty)

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

  it should "keep nested-schema updates of a transform in one report entry" in {
    val monitor = new ActivityMonitor[WorkflowExecutionReport]("workflowRun", initialValue = Some(WorkflowExecutionReport(workflowTask)))
    implicit val runContext: WorkflowRunContext = WorkflowRunContext(monitor, WorkflowTest.testWorkflow, UserContext.Empty)
    val nodeId = Identifier("transform")
    val taskContext = runContext.taskContext(nodeId, transformTask)

    taskContext.value.update(TransformReport(transformTask, entityCount = 1, isDone = true))
    taskContext.value.update(TransformReport(transformTask, entityCount = 2, isDone = true))

    monitor.value().taskReports should have size 1
    monitor.value().taskReports.head.report.entityCount shouldBe 2

    taskContext.value.update(TransformReport(transformTask, isDone = false))
    taskContext.value.update(TransformReport(transformTask, entityCount = 1, isDone = true))

    monitor.value().taskReports should have size 2
    monitor.value().taskReports.last.report.entityCount shouldBe 1
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
