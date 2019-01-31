package org.silkframework.serialization.json

import org.silkframework.rule.execution.TransformReport
import org.silkframework.rule.execution.TransformReport.{RuleError, RuleResult}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.util.Identifier
import play.api.libs.json.{JsObject, JsValue, Json}

object TransformSerializers {

  implicit object TransformReportJsonFormat extends WriteOnlyJsonFormat[TransformReport] {

    final val ENTITY_COUNTER = "entityCounter"
    final val ENTITY_ERROR_COUNTER = "entityErrorCounter"
    final val RULE_RESULTS = "ruleResults"

    final val ERROR_COUNT = "errorCount"
    final val SAMPLE_ERRORS = "sampleErrors"

    final val ENTITY = "entity"
    final val VALUE = "value"

    override def write(value: TransformReport)(implicit writeContext: WriteContext[JsValue]): JsValue = {
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
        VALUE -> ruleError.value
      )
    }

  }

}
