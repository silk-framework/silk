package controllers.linking.activeLearning

import controllers.core.util.JsonUtils
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.config.Prefixes
import org.silkframework.entity.ValueType
import org.silkframework.entity.paths.TypedPath
import org.silkframework.learning.active.comparisons.{ComparisonPair, ComparisonPairWithExamples, ComparisonPairs, PlainComparisonPair}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonFormat
import play.api.libs.json.{JsValue, Json, OFormat}


object JsonFormats {

  @Schema(description = "Holds the current comparison pairs.")
  case class ComparisonPairsFormat(@Schema(description = "The comparison pairs that have been suggested by the algorithm.")
                                   suggestedPairs: Seq[ComparisonPairFormat],
                                   @Schema(description = " The comparison paris that have been selected by the user.")
                                   selectedPairs: Seq[ComparisonPairFormat]) {
    def toComparisonPairs: ComparisonPairs = {
      ComparisonPairs(suggestedPairs.map(_.toComparisonPair), selectedPairs.map(_.toComparisonPair))
    }
  }

  object ComparisonPairsFormat {
    def apply(pairs: ComparisonPairs)
             (implicit prefixes: Prefixes): ComparisonPairsFormat = {
      ComparisonPairsFormat(pairs.suggestedPairs.map(ComparisonPairFormat(_)), pairs.selectedPairs.map(ComparisonPairFormat(_)))
    }
  }

  @Schema(description = "A single comparison pair")
  case class ComparisonPairFormat(@Schema(description = "The path from the first dataset.")
                                  source: TypedPathFormat,
                                  @Schema(description = "The path from the second dataset.")
                                  target: TypedPathFormat,
                                  @ArraySchema(schema = new Schema(description = "Example values for the source path.",
                                    implementation = classOf[String], required = false, nullable = true))
                                  sourceExamples: Option[Set[String]] = None,
                                  @ArraySchema(schema = new Schema(description = "Example values for the target path.",
                                    implementation = classOf[String], required = false, nullable = true))
                                  targetExamples: Option[Set[String]] = None) {
    def toComparisonPair: ComparisonPair = {
      ComparisonPairWithExamples(source.toTypedPath, target.toTypedPath, 0.0, sourceExamples.getOrElse(Set.empty), targetExamples.getOrElse(Set.empty))
    }
  }

  object ComparisonPairFormat {
    def apply(pair: ComparisonPair)
             (implicit prefixes: Prefixes): ComparisonPairFormat = {
      pair match {
        case PlainComparisonPair(source, target) =>
          ComparisonPairFormat(TypedPathFormat(source), TypedPathFormat(target), None, None)
        case ComparisonPairWithExamples(source, target, score, sourceExamples, targetExamples) =>
          ComparisonPairFormat(TypedPathFormat(source), TypedPathFormat(target), Some(sourceExamples), Some(targetExamples))
      }
    }
  }

  @Schema(description = "An entity path")
  case class TypedPathFormat(@Schema(description = "The serialized path", example = "path/name")
                             path: String,
                             @Schema(description = "Path label", example = "path label")
                             label: String,
                             @Schema(description = "The identifier of the value type", required = false, defaultValue = "StringValueType", example = "StringValueType")
                             valueType: Option[String] = None) {
    def toTypedPath: TypedPath = {
      TypedPath(path, valueType.map(id => ValueType.valueTypeById(id).right.get).getOrElse(ValueType.STRING))
    }
  }

  object TypedPathFormat {
    def apply(typedPath: TypedPath)
             (implicit prefixes: Prefixes): TypedPathFormat = {
      TypedPathFormat(typedPath.normalizedSerialization, typedPath.serialize(), Some(typedPath.valueType.id))
    }
  }

  implicit val typedPathFormat: OFormat[TypedPathFormat] = Json.format[TypedPathFormat]
  implicit val comparisonPairFormat: OFormat[ComparisonPairFormat] = Json.format[ComparisonPairFormat]
  implicit val comparisonPairsFormat: OFormat[ComparisonPairsFormat] = Json.format[ComparisonPairsFormat]

  class ComparisonPairsJsonFormat() extends JsonFormat[ComparisonPairs] {

    override def read(value: JsValue)(implicit readContext: ReadContext): ComparisonPairs = {
      JsonUtils.validateJsonFromValue[ComparisonPairsFormat](value).toComparisonPairs
    }

    override def write(value: ComparisonPairs)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      implicit val prefixes: Prefixes = writeContext.prefixes
      Json.toJson(ComparisonPairsFormat(value))
    }
  }

}
