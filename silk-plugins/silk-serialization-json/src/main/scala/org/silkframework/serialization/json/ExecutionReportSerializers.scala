package org.silkframework.serialization.json

import org.silkframework.execution.report.{EntitySample, SampleEntities, SampleEntitiesSchema, Stacktrace}
import org.silkframework.execution.{ExecutionReport, SimpleExecutionReport}
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.TransformReport.{RuleError, RuleResult}
import org.silkframework.rule.execution.{Linking, TransformReport}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.EntitySerializers.PairEntitySchemaJsonFormat
import org.silkframework.serialization.json.ExecutionReportSerializers.Keys._
import org.silkframework.serialization.json.JsonHelpers._
import org.silkframework.serialization.json.JsonSerializers.{GenericTaskJsonFormat, TaskJsonFormat, TransformSpecJsonFormat, toJsonOpt}
import org.silkframework.serialization.json.LinkingSerializers.LinkJsonFormat
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
          summary = arrayValue(value, SUMMARY).value.map(deserializeValue).toIndexedSeq,
          warnings = arrayValue(value, WARNINGS).value.map(_.as[String]).toIndexedSeq,
          error = stringValueOption(value, ERROR),
          operation = stringValueOption(value, OPERATION),
          operationDesc = stringValueOption(value, OPERATION_DESC).getOrElse(ExecutionReport.DEFAULT_OPERATION_DESC),
          isDone = booleanValueOption(value, IS_DONE).getOrElse(true),
          entityCount = numberValueOption(value, ENTITY_COUNT).map(_.intValue).getOrElse(0)
        )
      }
    }

    def serializeBasicValues(value: ExecutionReport)(implicit writeContext: WriteContext[JsValue]): JsObject = {
      Json.obj(
        LABEL -> value.task.label(),
        OPERATION -> value.operation,
        OPERATION_DESC -> value.operationDesc,
        TASK -> GenericTaskJsonFormat.write(value.task),
        SUMMARY -> value.summary.map(serializeValue),
        WARNINGS -> value.warnings,
        ERROR -> value.error,
        IS_DONE -> value.isDone,
        ENTITY_COUNT -> value.entityCount,
        OUTPUT_ENTITIES_SAMPLE -> value.sampleOutputEntities
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

  implicit object LinkingJsonFormat extends WriteOnlyJsonFormat[Linking] {
    final val LINKS = "links"
    final val STATISTICS = "statistics"
    final val ENTITY_SCHEMATA = "entitySchemata"

    override def write(value: Linking)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val firstEntityOption = value.links.headOption.flatMap(_.entities)
      val entitySchemataOption = firstEntityOption.map(_.map(_.schema))
      val linkFormat = new LinkJsonFormat(Some(value.rule))

      ExecutionReportJsonFormat.serializeBasicValues(value) ++
        Json.obj(
          LINKS -> value.links.map(linkFormat.write),
          STATISTICS -> Json.obj(
            "sourceEntities" -> value.statistics.entityCount.source,
            "targetEntities" -> value.statistics.entityCount.target,
            "linkCount" -> value.links.size
          ),
          ENTITY_SCHEMATA -> entitySchemataOption.map(PairEntitySchemaJsonFormat.write)
        )
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
        globalErrors = arrayValue(value, GLOBAL_ERRORS).value.map(_.as[String]).toIndexedSeq,
        isDone = booleanValueOption(value, IS_DONE).getOrElse(true),
        error = stringValueOption(value, ERROR)
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
        sampleErrors = arrayValue(value, SAMPLE_ERRORS).value.map(readRuleError).toIndexedSeq
      )
    }

    private def writeRuleError(ruleError: RuleError): JsValue = {
      Json.obj(
        ENTITY -> ruleError.entity,
        VALUES -> ruleError.value,
        ERROR -> ruleError.message
      ) ++ ruleError.exception.map(ex => Json.obj(STACKTRACE -> Json.toJson(Stacktrace.fromException(ex)))).getOrElse(JsObject.empty)
    }

    private def readRuleError(value: JsValue): RuleError = {
      RuleError(
        entity = stringValue(value, ENTITY),
        value = arrayValue(value, VALUES).as[Seq[Seq[String]]],
        message = stringValue(value, ERROR),
        // We ignore operator ID and exceptions in serialized execution reports. These are currently only relevant immediately.
        operatorId = None, exception = None
      )
    }
  }

  implicit val stacktraceJsonFormat: Format[Stacktrace] = Json.format[Stacktrace]
  implicit val sampleEntitiesSchemaJsonFormat: Format[SampleEntitiesSchema] = Json.format[SampleEntitiesSchema]
  implicit val entitySampleJsonFormat: Format[EntitySample] = Json.format[EntitySample]
  implicit val sampleEntitiesJsonFormat: Format[SampleEntities] = Json.format[SampleEntities]

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
        taskReports = taskReports.toIndexedSeq,
        isDone = booleanValueOption(value, IS_DONE).getOrElse(true)
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
    final val OPERATION_DESC = "operationDesc"
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

    final val STACKTRACE = "stacktrace"

    final val OUTPUT_ENTITIES_SAMPLE = "outputEntitiesSample"
  }

}
