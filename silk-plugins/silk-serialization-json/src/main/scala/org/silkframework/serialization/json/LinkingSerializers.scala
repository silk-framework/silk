package org.silkframework.serialization.json

import org.silkframework.entity.Link
import org.silkframework.rule.LinkageRule
import org.silkframework.rule.evaluation._
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.EntitySerializers.{EntityJsonFormat, PairJsonFormat}
import org.silkframework.serialization.json.JsonHelpers._
import play.api.libs.json.{JsArray, JsValue, Json}

/**
  *
  */
object LinkingSerializers {

  class LinkJsonFormat(rule: Option[LinkageRule], writeEntities: Boolean = false, writeEntitySchema: Boolean = false) extends JsonFormat[Link] {
    import LinkJsonFormat._

    final val SOURCE = "source"
    final val TARGET = "target"
    final val CONFIDENCE = "confidence"
    final val ENTITIES = "entities"

    private val entityPairFormat = new PairJsonFormat()(new EntityJsonFormat(includeSchema = writeEntitySchema))

    override def read(value: JsValue)(implicit readContext: ReadContext): Link = {
      Link(
        source = stringValue(value, SOURCE),
        target = stringValue(value, TARGET),
        confidence = numberValueOption(value, CONFIDENCE).map(_.doubleValue),
        entities = optionalValue(value, ENTITIES).map(entityPairFormat.read)
      )
    }

    override def write(link: Link)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val evaluationDetails = rule.flatMap(r => link.entities.flatMap(entities => DetailedEvaluator(r, entities).details))

      var json =
        Json.obj(
          SOURCE -> link.source,
          TARGET -> link.target,
          CONFIDENCE -> link.confidence,
          RULE_VALUES -> evaluationDetails.map(ConfidenceJsonFormat.write)
        )

      if(writeEntities) {
        for(entities <- link.entities) {
          json += (ENTITIES -> entityPairFormat.write(entities))
        }
      }

      json
    }
  }

  implicit object LinkJsonFormat extends LinkJsonFormat(None, writeEntities = false, writeEntitySchema = false) {
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
}
