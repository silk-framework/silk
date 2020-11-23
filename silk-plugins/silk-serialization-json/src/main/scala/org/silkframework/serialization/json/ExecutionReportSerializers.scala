package org.silkframework.serialization.json

import org.silkframework.execution.{ExecutionReport, SimpleExecutionReport}
import org.silkframework.rule.execution.TransformReport
import org.silkframework.rule.execution.TransformReport.{RuleError, RuleResult}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonHelpers._
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowExecutionReport, WorkflowExecutionReportWithProvenance}
import play.api.libs.json._
import ExecutionReportSerializers.Keys._
import org.silkframework.rule.TransformSpec
import org.silkframework.serialization.json.JsonSerializers.{GenericTaskJsonFormat, TaskJsonFormat, TaskSpecJsonFormat, TransformSpecJsonFormat}
import WorkflowSerializers.WorkflowJsonFormat

object ExecutionReportSerializers {

  implicit object ExecutionReportJsonFormat extends JsonFormat[ExecutionReport] {

    override def write(value: ExecutionReport)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      value match {
        case transformReport: TransformReport =>
          TransformReportJsonFormat.write(transformReport)
        case workflowReport: WorkflowExecutionReport =>
          WorkflowExecutionReportJsonFormat.write(workflowReport)
        case workflowReportWithProvenance: WorkflowExecutionReportWithProvenance =>
          WorkflowExecutionReportJsonFormat.write(workflowReportWithProvenance.report)
        case _ =>
          serializeBasicValues(value)
      }
    }

    override def read(value: JsValue)(implicit readContext: ReadContext): ExecutionReport = {
      if((value \ RULE_RESULTS).isDefined) {
        TransformReportJsonFormat.read(value)
      } else if((value \ TASK_REPORTS).isDefined) {
        WorkflowExecutionReportJsonFormat.read(value)
      } else {
        SimpleExecutionReport(
          task = GenericTaskJsonFormat.read(requiredValue(value, TASK)),
          summary = arrayValue(value, SUMMARY).value.map(deserializeValue),
          warnings = arrayValue(value, WARNINGS).value.map(_.as[String])
        )
      }
    }

    def serializeBasicValues(value: ExecutionReport)(implicit writeContext: WriteContext[JsValue]): JsObject = {
      Json.obj(
        LABEL -> value.task.taskLabel(),
        TASK -> GenericTaskJsonFormat.write(value.task),
        SUMMARY -> value.summary.map(serializeValue),
        WARNINGS -> value.warnings
      )
    }

    private def serializeValue(value: (String, String)): JsValue = {
      Json.obj(
        KEY -> value._1,
        VALUE -> value._2
      )
    }

    private def deserializeValue(value: JsValue): (String, String) = {
      (stringValue(value, KEY), stringValue(value, VALUE))
    }
  }

  implicit object TransformReportJsonFormat extends JsonFormat[TransformReport] {

    override def write(value: TransformReport)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      ExecutionReportJsonFormat.serializeBasicValues(value) ++
        Json.obj(
          ENTITY_COUNTER -> value.entityCounter,
          ENTITY_ERROR_COUNTER -> value.entityErrorCounter,
          RULE_RESULTS -> writeRuleResults(value.ruleResults),
          GLOBAL_ERRORS -> value.globalErrors
        )
    }

    override def read(value: JsValue)(implicit readContext: ReadContext): TransformReport = {
      implicit val taskFormat = new TaskJsonFormat[TransformSpec]()
      TransformReport(
        task = taskFormat.read(requiredValue(value, TASK)),
        entityCounter = numberValue(value, ENTITY_COUNTER).longValue,
        entityErrorCounter = numberValue(value, ENTITY_ERROR_COUNTER).longValue,
        ruleResults = readRuleResults(objectValue(value, RULE_RESULTS)),
        globalErrors = arrayValue(value, GLOBAL_ERRORS).value.map(_.as[String])
      )
    }

    private def writeRuleResults(ruleResults: Map[Identifier, RuleResult]): JsValue = {
      JsObject(
        for((ruleId, ruleResult) <- ruleResults) yield {
          (ruleId.toString, writeRuleResult(ruleResult))
        }
      )
    }

    private def readRuleResults(value: JsObject): Map[Identifier, RuleResult] = {
      for((key, value) <- value.value) yield {
        (Identifier(key) -> readRuleResult(value))
      }
    }.toMap

    private def writeRuleResult(ruleResult: RuleResult): JsValue = {
      Json.obj(
        ERROR_COUNT -> ruleResult.errorCount,
        SAMPLE_ERRORS -> ruleResult.sampleErrors.map(writeRuleError)
      )
    }

    private def readRuleResult(value: JsValue): RuleResult = {
      RuleResult(
        errorCount = numberValue(value, ERROR_COUNT).longValue,
        sampleErrors = arrayValue(value, SAMPLE_ERRORS).value.map(readRuleError)
      )
    }

    private def writeRuleError(ruleError: RuleError): JsValue = {
      Json.obj(
        ENTITY -> ruleError.entity,
        VALUES -> ruleError.value,
        ERROR -> ruleError.message
      )
    }

    private def readRuleError(value: JsValue): RuleError = {
      RuleError(
        entity = stringValue(value, ENTITY),
        value = arrayValue(value, VALUES).as[Seq[Seq[String]]],
        message = stringValue(value, ERROR)
      )
    }

  }

  implicit object WorkflowExecutionReportJsonFormat extends JsonFormat[WorkflowExecutionReport] {

    override def write(value: WorkflowExecutionReport)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      ExecutionReportJsonFormat.serializeBasicValues(value) ++
        Json.obj(
          TASK_REPORTS -> JsObject(value.taskReports.map(serializeTaskReport))
        )
    }

    override def read(value: JsValue)(implicit readContext: ReadContext): WorkflowExecutionReport = {
      implicit val taskFormat = new TaskJsonFormat[Workflow]()
      val taskReports =
        for((key, value) <- objectValue(value, TASK_REPORTS).value) yield {
          (Identifier(key) -> ExecutionReportJsonFormat.read(value))
        }
      WorkflowExecutionReport(
        task = taskFormat.read(requiredValue(value, TASK)),
        taskReports = taskReports.toMap
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

  object Keys {

    final val TASK = "task"
    final val LABEL = "label"
    final val SUMMARY = "summary"
    final val WARNINGS = "warnings"

    final val KEY = "key"
    final val VALUE = "value"

    final val TASK_REPORTS = "taskReports"

    final val ENTITY_COUNTER = "entityCounter"
    final val ENTITY_ERROR_COUNTER = "entityErrorCounter"
    final val RULE_RESULTS = "ruleResults"
    final val GLOBAL_ERRORS = "globalErrors"

    final val ERROR_COUNT = "errorCount"
    final val SAMPLE_ERRORS = "sampleErrors"

    final val ENTITY = "entity"
    final val VALUES = "values"
    final val ERROR = "error"

  }

}
