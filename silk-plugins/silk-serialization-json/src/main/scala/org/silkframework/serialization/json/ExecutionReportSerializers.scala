package org.silkframework.serialization.json

import org.silkframework.execution.{ExecutionReport, SimpleExecutionReport}
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.TransformReport
import org.silkframework.rule.execution.TransformReport.{RuleError, RuleResult}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.ExecutionReportSerializers.Keys._
import org.silkframework.serialization.json.JsonHelpers._
import org.silkframework.serialization.json.JsonSerializers.{GenericTaskJsonFormat, TaskJsonFormat, TransformSpecJsonFormat}
import org.silkframework.serialization.json.WorkflowSerializers.WorkflowJsonFormat
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowExecutionReport, WorkflowExecutionReportWithProvenance, WorkflowTaskReport}
import play.api.libs.json._

import java.time.Instant

object ExecutionReportSerializers {

  implicit object ExecutionReportJsonFormat extends JsonFormat[ExecutionReport] {

    override def write(value: ExecutionReport)(implicit writeContext: WriteContext[JsValue]): JsObject = {
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
          warnings = arrayValue(value, WARNINGS).value.map(_.as[String]),
          operation = stringValueOption(value, OPERATION),
          isDone = booleanValueOption(value, IS_DONE).getOrElse(true),
          entityCount = numberValueOption(value, ENTITY_COUNT).map(_.intValue).getOrElse(0)
        )
      }
    }

    def serializeBasicValues(value: ExecutionReport)(implicit writeContext: WriteContext[JsValue]): JsObject = {
      Json.obj(
        LABEL -> value.task.taskLabel(),
        OPERATION -> value.operation,
        TASK -> GenericTaskJsonFormat.write(value.task),
        SUMMARY -> value.summary.map(serializeValue),
        WARNINGS -> value.warnings,
        IS_DONE -> value.isDone,
        ENTITY_COUNT -> value.entityCount
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

    override def write(value: TransformReport)(implicit writeContext: WriteContext[JsValue]): JsObject = {
      ExecutionReportJsonFormat.serializeBasicValues(value) ++
        Json.obj(
          ENTITY_COUNTER -> value.entityCount,
          ENTITY_ERROR_COUNTER -> value.entityErrorCount,
          RULE_RESULTS -> writeRuleResults(value.ruleResults),
          GLOBAL_ERRORS -> value.globalErrors
        )
    }

    override def read(value: JsValue)(implicit readContext: ReadContext): TransformReport = {
      implicit val taskFormat = new TaskJsonFormat[TransformSpec]()
      TransformReport(
        task = taskFormat.read(requiredValue(value, TASK)),
        entityCount = numberValue(value, ENTITY_COUNTER).intValue,
        entityErrorCount = numberValue(value, ENTITY_ERROR_COUNTER).intValue,
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

  implicit object WorkflowTaskReportJsonFormat extends JsonFormat[WorkflowTaskReport] {

    override def read(value: JsValue)(implicit readContext: ReadContext): WorkflowTaskReport = {
      WorkflowTaskReport(
        nodeId = stringValue(value, NODE),
        report = ExecutionReportJsonFormat.read(value),
        timestamp = Instant.ofEpochMilli(numberValue(value, TIMESTAMP).longValue)
      )
    }

    override def write(value: WorkflowTaskReport)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val reportJson = value.report match {
        case t: TransformReport =>
          TransformReportJsonFormat.write(t)
        case report: ExecutionReport =>
          ExecutionReportJsonFormat.write(report)
      }

      reportJson +
        (NODE -> JsString(value.nodeId.toString)) +
        (TIMESTAMP -> JsNumber(value.timestamp.toEpochMilli))
    }

    def writeSummary(value: WorkflowTaskReport)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val report = value.report
      Json.obj(
        NODE -> value.nodeId.toString,
        TIMESTAMP -> value.timestamp.toEpochMilli,
        OPERATION -> report.operation,
        WARNINGS -> report.warnings,
        IS_DONE -> report.isDone,
        ENTITY_COUNT -> report.entityCount
      )
    }
  }

  implicit object WorkflowExecutionReportJsonFormat extends JsonFormat[WorkflowExecutionReport] {

    override def write(value: WorkflowExecutionReport)(implicit writeContext: WriteContext[JsValue]): JsObject = {
      ExecutionReportJsonFormat.serializeBasicValues(value) +
        (TASK_REPORTS -> JsArray(value.taskReports.map(WorkflowTaskReportJsonFormat.write)))
    }

    override def read(value: JsValue)(implicit readContext: ReadContext): WorkflowExecutionReport = {
      implicit val taskFormat = new TaskJsonFormat[Workflow]()
      val taskReports = requiredValue(value, TASK_REPORTS) match {
        case jsArray: JsArray =>
          for(report <- jsArray.value) yield {
            WorkflowTaskReportJsonFormat.read(report)
          }
        case jsObject: JsObject =>
          // deprecated format
          for((key, objValue) <- jsObject.value.toSeq) yield {
            WorkflowTaskReport(
              nodeId = Identifier(key),
              report = ExecutionReportJsonFormat.read(objValue)
            )
          }
        case _ =>
          throw JsonParseException(s"$TASK_REPORTS must be an array or an object")
      }

      WorkflowExecutionReport(
        task = taskFormat.read(requiredValue(value, TASK)),
        taskReports = IndexedSeq(taskReports: _*)
      )
    }
  }

  implicit object WorkflowExecutionReportWithProvenanceJsonFormat extends WriteOnlyJsonFormat[WorkflowExecutionReportWithProvenance] {

    override def write(value: WorkflowExecutionReportWithProvenance)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      WorkflowExecutionReportJsonFormat.write(value.report)
    }
  }

  object Keys {

    final val OPERATION = "operation"
    final val TASK = "task"
    final val LABEL = "label"
    final val SUMMARY = "summary"
    final val WARNINGS = "warnings"
    final val IS_DONE = "isDone"
    final val ENTITY_COUNT = "entityCount"
    final val NODE = "nodeId" // node id within a workflow
    final val TIMESTAMP = "timestamp"

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
