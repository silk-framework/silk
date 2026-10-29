package org.silkframework.workspace.activity.workflow

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.PlainTask
import org.silkframework.execution.{OperationType, SimpleExecutionReport}
import org.silkframework.util.Identifier

class WorkflowExecutionReportTest extends AnyFlatSpec with Matchers {

  private val parentTask = PlainTask("parentWf", WorkflowTest.testWorkflow)

  "currentReports" should "return the most recently updated report per node even if timestamps collide" in {
    val timestamp = java.time.Instant.now()
    val clear = SimpleExecutionReport(
      PlainTask("ds", WorkflowTest.testWorkflow), entityCount = 1, isDone = true, operationDesc = "dataset cleared")
    val write = SimpleExecutionReport(
      PlainTask("ds", WorkflowTest.testWorkflow), entityCount = 10, isDone = true, operationDesc = "entities written")
    val report = WorkflowExecutionReport(
      task = parentTask,
      taskReports = IndexedSeq(
        WorkflowTaskReport(Identifier("ds"), clear, version = 1, timestamp = timestamp),
        WorkflowTaskReport(Identifier("ds"), write, version = 2, timestamp = timestamp)))

    report.currentReports().map(_.report.operationDesc) shouldBe Iterable("entities written")
  }

  it should "assign strictly increasing versions across added and updated reports" in {
    val nodeId = Identifier("ds")
    def nodeReport(desc: String) = SimpleExecutionReport(
      PlainTask("ds", WorkflowTest.testWorkflow), entityCount = 1, isDone = true, operationDesc = desc)

    val report = WorkflowExecutionReport(task = parentTask)
      .addReport(nodeId, nodeReport("initial"))
      .updateReport(0, nodeId, nodeReport("dataset cleared"))
      .addReport(nodeId, nodeReport("entities written")) // e.g. a second write into the same dataset

    val versions = report.taskReports.map(_.version)
    versions shouldBe versions.sorted
    versions.distinct shouldBe versions
    // The most recent change must win, which relies on unique versions
    report.currentReports().map(_.report.operationDesc) shouldBe Iterable("entities written")
  }

  "addFailedNode" should "keep a nested workflow's task reports when the node fails" in {
    val leafReport = SimpleExecutionReport(
      PlainTask("leaf", WorkflowTest.testWorkflow), entityCount = 3, isDone = true, operationDesc = "entities written")
    val nestedWorkflowReport = WorkflowExecutionReport(
      task = PlainTask("childWf", WorkflowTest.testWorkflow),
      taskReports = IndexedSeq(WorkflowTaskReport(Identifier("leaf"), leafReport)),
      isDone = true)
    val parent = WorkflowExecutionReport(
      task = parentTask,
      taskReports = IndexedSeq(WorkflowTaskReport(Identifier("childWf"), nestedWorkflowReport)))

    val failed = parent.addFailedNode(Identifier("childWf"), new RuntimeException("boom"))

    val nodeReport = failed.taskReports.find(_.nodeId == Identifier("childWf")).get.report
    // The nested workflow report must survive as a WorkflowExecutionReport with its child reports intact.
    nodeReport shouldBe a[WorkflowExecutionReport]
    nodeReport.asInstanceOf[WorkflowExecutionReport].taskReports.map(_.nodeId.toString) should contain("leaf")
    nodeReport.error shouldBe Some("boom")
    nodeReport.isDone shouldBe true
  }

  it should "downgrade a plain node report to a SimpleExecutionReport (default behavior)" in {
    val plain = SimpleExecutionReport(
      PlainTask("ds", WorkflowTest.testWorkflow), entityCount = 5, isDone = false, operationDesc = "entities read",
      operationType = OperationType.Read, title = Some("Read 5 entities of type Person"))
    val parent = WorkflowExecutionReport(
      task = parentTask,
      taskReports = IndexedSeq(WorkflowTaskReport(Identifier("ds"), plain)))

    val failed = parent.addFailedNode(Identifier("ds"), new RuntimeException("kaboom"))

    val nodeReport = failed.taskReports.find(_.nodeId == Identifier("ds")).get.report
    nodeReport shouldBe a[SimpleExecutionReport]
    nodeReport.error shouldBe Some("kaboom")
    nodeReport.isDone shouldBe true
    // The failed report must keep the descriptive title and operation type
    nodeReport.title shouldBe Some("Read 5 entities of type Person")
    nodeReport.operationType shouldBe OperationType.Read
  }
}
