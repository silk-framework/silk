package org.silkframework.rule.execution

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.PlainTask
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.ActivityMonitor

class TransformReportBuilderTest extends AnyFlatSpec with Matchers {

  behavior of "TransformReportBuilder"

  it should "keep a failed execution final when further output tables are closed" in {
    val task = PlainTask("transform", TransformSpec.empty)
    val context = new ActivityMonitor[TransformReport](task.id)
    val report = new TransformReportBuilder(task, context, outputTableCount = 2)

    report.outputTableCompleted()
    context.value().isDone shouldBe false

    report.executionFailed("Transform failed")
    val failedReport = context.value()
    failedReport.isDone shouldBe true
    failedReport.error shouldBe Some("Transform failed")

    report.outputTableCompleted()
    context.value() shouldBe failedReport
  }
}
