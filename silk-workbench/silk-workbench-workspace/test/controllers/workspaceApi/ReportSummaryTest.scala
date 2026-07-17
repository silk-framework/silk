package controllers.workspaceApi

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.PlainTask
import org.silkframework.execution.SimpleExecutionReport
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.WorkflowTaskReport

/**
  * Tests the aggregation of a node's per-execution reports into a single summary.
  */
class ReportSummaryTest extends AnyFlatSpec with Matchers {

  behavior of "ReportSummary"

  private val task = PlainTask("task", TestCustomTask())
  private val nodeId = Identifier("dataset1")

  private def report(operation: String, operationDesc: String, entityCount: Int, warnings: Seq[String] = Seq.empty) = {
    SimpleExecutionReport(task, summary = Seq.empty, warnings = warnings, error = None, isDone = true,
      entityCount = entityCount, operation = Some(operation), operationDesc = operationDesc)
  }

  it should "sum the entity counts of all executions of the same operation" in {
    val clear = report("clear dataset", "dataset cleared", 1)
    val write1 = report("write", "entities written", 10, warnings = Seq("some warning"))
    val write2 = report("write", "entities written", 10)

    val summary = ReportSummary(WorkflowTaskReport(nodeId, write2), Seq(clear, write1, write2))
    summary.entityCount mustBe 20
    summary.operationDesc mustBe "entities written"
    summary.warnings mustBe Seq("some warning")
    summary.isDone mustBe true
  }

  it should "use a plural operation description when the latest execution has a singular one" in {
    val write1 = report("write", "entities written", 10)
    val write2 = report("write", "entity written", 1)

    val summary = ReportSummary(WorkflowTaskReport(nodeId, write2), Seq(write1, write2))
    summary.entityCount mustBe 11
    summary.operationDesc mustBe "entities written"
  }

  it should "keep the report of a single execution unchanged" in {
    val write = report("write", "entity written", 1)

    val summary = ReportSummary(WorkflowTaskReport(nodeId, write), Seq(write))
    summary.entityCount mustBe 1
    summary.operationDesc mustBe "entity written"
  }
}
