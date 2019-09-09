package org.silkframework.serialization.json

import org.silkframework.entity.Link
import org.silkframework.rule.LinkageRule
import org.silkframework.rule.evaluation._
import org.silkframework.rule.execution.Linking
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.EntitySerializers.PairEntitySchemaJsonFormat
import org.silkframework.serialization.json.JsonHelpers.{mustBeDefined, mustBeJsArray, numberValueOption, stringValue}
import org.silkframework.serialization.json.JsonSerializers.{fromJson, toJson}
import play.api.libs.json.{JsArray, JsValue, Json}

object LinkingSerializers {

  implicit object LinkingJsonFormat extends WriteOnlyJsonFormat[Linking] {
    final val LINKS = "links"
    final val STATISTICS = "statistics"
    final val ENTITY_SCHEMATA = "entitySchemata"

    override def write(value: Linking)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val firstEntityOption = value.links.headOption.flatMap(_.entities)
      val entitySchemataOption = firstEntityOption.map(_.map(_.schema))
      val linkFormat = new LinkJsonFormat(Some(value.rule))

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

  final val RULE_VALUES = "ruleValues"
  class LinkJsonFormat(rule: Option[LinkageRule]) extends JsonFormat[Link] {
    final val SOURCE = "source"
    final val TARGET = "target"
    final val CONFIDENCE = "confidence"
    final val ENTITIES = "entities"

    override def read(value: JsValue)(implicit readContext: ReadContext): Link = {
      new Link(
        source = stringValue(value, SOURCE),
        target = stringValue(value, TARGET),
        confidence = numberValueOption(value, CONFIDENCE).map(_.doubleValue)
      )
    }

    override def write(link: Link)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val evaluationDetails = rule.flatMap(r => link.entities.flatMap(entities => DetailedEvaluator(r, entities).details))

      Json.obj(
        SOURCE -> link.source,
        TARGET -> link.target,
        CONFIDENCE -> link.confidence,
        // The entity values are also part of the rule values, so we don't write them currently: ENTITIES -> link.entities.map(entityPairFormat.write),
        RULE_VALUES -> evaluationDetails.map(ConfidenceJsonFormat.write)
      )
    }
  }

  implicit object LinkJsonFormat extends LinkJsonFormat(None)

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

    override def write(value: Value)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      value match {
        case TransformedValue(input, values, children, error) =>
          Json.obj(
            OPERATOR_ID -> input.id.toString,
            VALUES -> values,
            ERROR -> error.map(_.getMessage),
            CHILDREN -> children.map(write)
          )
        case InputValue(input, values, error) =>
          Json.obj(
            OPERATOR_ID -> input.id.toString,
            VALUES -> values,
            ERROR -> error.map(_.getMessage)
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
        positive = mustBeJsArray(mustBeDefined(value, POSITIVE))(_.value.map(fromJson[Link])).toSet,
        negative = mustBeJsArray(mustBeDefined(value, POSITIVE))(_.value.map(fromJson[Link])).toSet,
        unlabeled = mustBeJsArray(mustBeDefined(value, POSITIVE))(_.value.map(fromJson[Link])).toSet
      )
    }

    override def write(value: ReferenceLinks)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        POSITIVE -> JsArray(value.positive.toSeq.map(toJson(_))),
        NEGATIVE -> JsArray(value.negative.toSeq.map(toJson(_))),
        UNLABELED -> JsArray(value.unlabeled.toSeq.map(toJson(_)))
      )
    }
  }
}
