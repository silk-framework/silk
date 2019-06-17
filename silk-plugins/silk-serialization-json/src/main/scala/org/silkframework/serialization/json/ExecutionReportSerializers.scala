package org.silkframework.serialization.json

import org.silkframework.execution.ExecutionReport
import org.silkframework.rule.execution.TransformReport
import org.silkframework.rule.execution.TransformReport.{RuleError, RuleResult}
import org.silkframework.runtime.serialization.{Serialization, WriteContext}
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.{WorkflowExecutionReport, WorkflowExecutionReportWithProvenance}
import play.api.libs.json._

object ExecutionReportSerializers {

  implicit object ExecutionReportJsonFormat extends WriteOnlyJsonFormat[ExecutionReport] {

    override def write(value: ExecutionReport)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      serialize(value)
    }

    def serialize(value: ExecutionReport): JsObject = {
      Json.obj(
        "label" -> value.label,
        "summary" -> value.summary.map(serializeValue),
        "warning" -> value.warning
      )
    }

    private def serializeValue(value: (String, String)): JsValue = {
      Json.obj(
        "key" -> value._1,
        "value" -> value._2
      )
    }
  }

  implicit object TransformReportJsonFormat extends WriteOnlyJsonFormat[TransformReport] {

    final val ENTITY_COUNTER = "entityCounter"
    final val ENTITY_ERROR_COUNTER = "entityErrorCounter"
    final val RULE_RESULTS = "ruleResults"

    final val ERROR_COUNT = "errorCount"
    final val SAMPLE_ERRORS = "sampleErrors"

    final val ENTITY = "entity"
    final val VALUES = "values"
    final val ERROR = "error"

    override def write(value: TransformReport)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      ExecutionReportJsonFormat.serialize(value) ++
        Json.obj(
          ENTITY_COUNTER -> value.entityCounter,
          ENTITY_ERROR_COUNTER -> value.entityErrorCounter,
          RULE_RESULTS -> writeRuleResults(value.ruleResults)
        )
    }

    private def writeRuleResults(ruleResults: Map[Identifier, RuleResult]): JsValue = {
      JsObject(
        for((ruleId, ruleResult) <- ruleResults) yield {
          (ruleId.toString, writeRuleResult(ruleResult))
        }
      )
    }

    private def writeRuleResult(ruleResult: RuleResult): JsValue = {
      Json.obj(
        ERROR_COUNT -> ruleResult.errorCount,
        SAMPLE_ERRORS -> ruleResult.sampleErrors.map(writeRuleError)
      )
    }

    private def writeRuleError(ruleError: RuleError): JsValue = {
      Json.obj(
        ENTITY -> ruleError.entity,
        VALUES -> ruleError.value,
        ERROR -> ruleError.exception.getMessage
      )
    }

  }

  implicit object WorkflowExecutionReportJsonFormat extends WriteOnlyJsonFormat[WorkflowExecutionReport] {

    /**
      * Serializes a value.
      */
    override def write(value: WorkflowExecutionReport)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      ExecutionReportJsonFormat.serialize(value) ++
        Json.obj(
          "taskReports" -> JsObject(value.taskReports.map(serializeTaskReport))
        )
    }

    private def serializeTaskReport(taskAndReport: (Identifier, ExecutionReport))
                                   (implicit writeContext: WriteContext[JsValue]): (String, JsValue) = {
      val (task, report) = taskAndReport
      val reportJson = report match {
        case t: TransformReport =>
          TransformReportJsonFormat.write(t)
        case _ =>
          ExecutionReportJsonFormat.write(report)
      }
      (task.toString, reportJson)
    }
  }

  implicit object WorkflowExecutionReportWithProvenanceJsonFormat extends WriteOnlyJsonFormat[WorkflowExecutionReportWithProvenance] {

    override def write(value: WorkflowExecutionReportWithProvenance)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      WorkflowExecutionReportJsonFormat.write(value.report)
    }
  }

}
