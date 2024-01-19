package org.silkframework.serialization.json

import org.silkframework.entity.{Link, LinkWithDecision}
import org.silkframework.execution.report.Stacktrace
import org.silkframework.rule.LinkageRule
import org.silkframework.rule.evaluation._
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.EntitySerializers.{EntityJsonFormat, PairJsonFormat}
import org.silkframework.serialization.json.JsonHelpers._
import org.silkframework.serialization.json.ExecutionReportSerializers.stacktraceJsonFormat
import play.api.libs.json._

object LinkingSerializers {

  class LinkJsonFormat(rule: Option[LinkageRule],
                       writeEntities: Boolean = false,
                       writeEntitySchema: Boolean = false,
                       distinctValues: Boolean = false) extends JsonFormat[Link] {
    import LinkJsonFormat._

    final val SOURCE = "source"
    final val TARGET = "target"
    final val CONFIDENCE = "confidence"
    final val ENTITIES = "entities"
    final val DECISION = "decision"

    private val entityPairFormat = new PairJsonFormat()(new EntityJsonFormat(includeSchema = writeEntitySchema, distinctValues))

    override def read(value: JsValue)(implicit readContext: ReadContext): Link = {
      Link(
        source = stringValue(value, SOURCE),
        target = stringValue(value, TARGET),
        confidence = numberValueOption(value, CONFIDENCE).map(_.doubleValue),
        entities = optionalValue(value, ENTITIES).map(entityPairFormat.read)
      )
    }

    override def write(link: Link)(implicit writeContext: WriteContext[JsValue]): JsObject = {
      var json =
        Json.obj(
          SOURCE -> link.source,
          TARGET -> link.target,
          CONFIDENCE -> link.confidence
        )

      // Add entities
      if (writeEntities) {
        for (entities <- link.entities) {
          json += (ENTITIES -> entityPairFormat.write(entities))
        }
      }

      // Add decision
      link match {
        case linkWithDecision: LinkWithDecision =>
          json += (DECISION -> JsString(linkWithDecision.decision.getId))
        case _ =>
      }

      // Add evaluation details
      link match {
        case linkWithDetails: LinkWithEvaluation =>
          json += (RULE_VALUES -> ConfidenceJsonFormat.write(linkWithDetails.details))
        case _ =>
          // Link does not provide evaluation details, need to evaluate it first
          for (linkingRule <- rule; entities <- link.entities) {
            val details = DetailedEvaluator(linkingRule, entities).details
            json += (RULE_VALUES -> ConfidenceJsonFormat.write(details))
          }
      }

      json
    }
  }

  implicit object LinkJsonFormat extends LinkJsonFormat(None, writeEntities = false, writeEntitySchema = false, distinctValues = false) {
    final val RULE_VALUES = "ruleValues"
  }

  implicit object ConfidenceJsonFormat extends WriteOnlyJsonFormat[Confidence] {
    final val SCORE = "score"
    final val OPERATOR_ID = "operatorId"
    final val CHILDREN = "children"
    final val SOURCE_VALUE = "sourceValue"
    final val TARGET_VALUE = "targetValue"

    override def write(value: Confidence)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      value match {
        case SimpleConfidence(score) =>
          Json.obj(
            SCORE -> score
          )
        case AggregatorConfidence(score, aggregation, children) =>
          Json.obj(
            OPERATOR_ID -> aggregation.id.toString,
            SCORE -> score,
            CHILDREN -> children.map(write)
          )
        case ComparisonConfidence(score, comparison, sourceValue, targetValue) =>
          Json.obj(
            OPERATOR_ID -> comparison.id.toString,
            SCORE -> score,
            SOURCE_VALUE -> ValueJsonFormat.write(sourceValue),
            TARGET_VALUE -> ValueJsonFormat.write(targetValue)
          )
      }
    }
  }

  implicit object ValueJsonFormat extends WriteOnlyJsonFormat[Value] {
    final val OPERATOR_ID = "operatorId"
    final val CHILDREN = "children"
    final val VALUES = "values"
    final val ERROR = "error"
    final val STACKTRACE = "stacktrace"

    override def write(value: Value)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      value match {
        case TransformedValue(input, values, children, error) =>
          Json.obj(
            OPERATOR_ID -> input.id.toString,
            VALUES -> values,
            ERROR -> error.map(_.getMessage),
            STACKTRACE -> error.map(ex => Json.toJson(Stacktrace.fromException(ex))),
            CHILDREN -> children.map(write)
          )
        case InputValue(input, values, error) =>
          Json.obj(
            OPERATOR_ID -> input.id.toString,
            VALUES -> values,
            ERROR -> error.map(_.getMessage),
            STACKTRACE -> error.map(ex => Json.toJson(Stacktrace.fromException(ex)))
          )
      }
    }
  }

  implicit object ReferenceLinksJsonFormat extends JsonFormat[ReferenceLinks] {
    final val POSITIVE = "positive"
    final val NEGATIVE = "negative"
    final val UNLABELED = "unlabeled"

    override def read(value: JsValue)(implicit readContext: ReadContext): ReferenceLinks = {
      ReferenceLinks(
        positive = mustBeJsArray(mustBeDefined(value, POSITIVE))(_.value.map(LinkJsonFormat.read)).toSet,
        negative = mustBeJsArray(mustBeDefined(value, NEGATIVE))(_.value.map(LinkJsonFormat.read)).toSet,
        unlabeled = mustBeJsArray(mustBeDefined(value, UNLABELED))(_.value.map(LinkJsonFormat.read)).toSet
      )
    }

    override def write(value: ReferenceLinks)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        POSITIVE -> JsArray(value.positive.toSeq.map(LinkJsonFormat.write)),
        NEGATIVE -> JsArray(value.negative.toSeq.map(LinkJsonFormat.write)),
        UNLABELED -> JsArray(value.unlabeled.toSeq.map(LinkJsonFormat.write))
      )
    }
  }

  implicit object DetailedEntityJsonFormat extends WriteOnlyJsonFormat[DetailedEntity] {
    final val URIS = "uris"
    final val VALUES = "values"

    override def write(detailedEntity: DetailedEntity)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        URIS -> detailedEntity.uris,
        VALUES -> detailedEntity.values.map(ValueJsonFormat.write)
      )
    }
  }
}
