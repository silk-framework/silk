package controllers.linking.activeLearning

import controllers.core.util.JsonUtils
import org.silkframework.entity.ValueType
import org.silkframework.entity.paths.TypedPath
import org.silkframework.learning.active.comparisons.{ComparisonPair, ComparisonPairs}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonFormat
import play.api.libs.json.{JsValue, Json, OFormat}

//TODO add Swagger annotations etc.
object JsonFormats {

  case class ComparisonPairsFormat(suggestedPairs: Seq[ComparisonPairFormat],
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

  case class ComparisonPairFormat(source: TypedPathFormat, target: TypedPathFormat) {
    def toComparisonPair: ComparisonPair = {
      ComparisonPair(source.toTypedPath, target.toTypedPath)
    }
  }

  object ComparisonPairFormat {
    def apply(pair: ComparisonPair): ComparisonPairFormat = {
      ComparisonPairFormat(TypedPathFormat(pair.source), TypedPathFormat(pair.target))
    }
  }

  case class TypedPathFormat(path: String, valueType: String) {
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
