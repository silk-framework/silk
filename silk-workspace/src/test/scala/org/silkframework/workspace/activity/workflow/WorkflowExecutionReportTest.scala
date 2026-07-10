package org.silkframework.workspace.activity.workflow

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.PlainTask
import org.silkframework.execution.SimpleExecutionReport
import org.silkframework.util.Identifier

class WorkflowExecutionReportTest extends AnyFlatSpec with Matchers {

  private val parentTask = PlainTask("parentWf", WorkflowTest.testWorkflow)

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
      PlainTask("ds", WorkflowTest.testWorkflow), entityCount = 5, isDone = false, operationDesc = "entities read")
    val parent = WorkflowExecutionReport(
      task = parentTask,
      taskReports = IndexedSeq(WorkflowTaskReport(Identifier("ds"), plain)))

    val failed = parent.addFailedNode(Identifier("ds"), new RuntimeException("kaboom"))

    val nodeReport = failed.taskReports.find(_.nodeId == Identifier("ds")).get.report
    nodeReport shouldBe a[SimpleExecutionReport]
    nodeReport.error shouldBe Some("kaboom")
    nodeReport.isDone shouldBe true
  }
}
