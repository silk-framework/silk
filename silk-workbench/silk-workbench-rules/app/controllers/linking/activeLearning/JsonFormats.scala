package controllers.linking.activeLearning

import controllers.core.util.JsonUtils
import io.swagger.v3.oas.annotations.media.Schema
import org.silkframework.entity.ValueType
import org.silkframework.entity.paths.TypedPath
import org.silkframework.learning.active.comparisons.{ComparisonPair, ComparisonPairs}
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
    def apply(pairs: ComparisonPairs): ComparisonPairsFormat = {
      ComparisonPairsFormat(pairs.suggestedPairs.map(ComparisonPairFormat(_)), pairs.selectedPairs.map(ComparisonPairFormat(_)))
    }
  }

  @Schema(description = "A single comparison pair")
  case class ComparisonPairFormat(@Schema(description = "The path from the first dataset.")
                                  source: TypedPathFormat,
                                  @Schema(description = "The path from the second dataset.")
                                  target: TypedPathFormat,
                                  //TODO doc
                                  sourceExamples: Seq[String],
                                  targetxamples: Seq[String]) {
    def toComparisonPair: ComparisonPair = {
      ComparisonPair(source.toTypedPath, target.toTypedPath)
    }
  }

  object ComparisonPairFormat {
    def apply(pair: ComparisonPair): ComparisonPairFormat = {
      ComparisonPairFormat(TypedPathFormat(pair.source), TypedPathFormat(pair.target))
    }
  }

  @Schema(description = "An entity path")
  case class TypedPathFormat(@Schema(description = "The serialized path", example = "path/name")
                             path: String,
                             @Schema(description = "The identifier of the value type", example = "StringValueType")
                             valueType: Option[String],
                             ) {
    def toTypedPath: TypedPath = {
      TypedPath(path, ValueType.valueTypeById(valueType).right.get)
    }
  }

  object TypedPathFormat {
    def apply(typedPath: TypedPath): TypedPathFormat = {
      TypedPathFormat(typedPath.normalizedSerialization, typedPath.valueType.id)
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
      Json.toJson(ComparisonPairsFormat(value))
    }
  }

}
