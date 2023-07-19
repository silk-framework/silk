package controllers.datasetApi.payloads

import org.silkframework.dataset.DatasetCharacteristics
import org.silkframework.dataset.DatasetCharacteristics.{SpecialPathInfo, SuggestedForEnum, SupportedPathExpressions}
import play.api.libs.json.{Format, Json}

object DatasetCharacteristicsPayload {
  implicit val suggestedForEnumFormat: Format[DatasetCharacteristics.SuggestedForEnum.Value] = Json.formatEnum(SuggestedForEnum)
  implicit val specialPathInfoFormat: Format[SpecialPathInfo] = Json.format[SpecialPathInfo]
  implicit val supportedPathExpressionsFormat: Format[SupportedPathExpressions] = Json.format[SupportedPathExpressions]
  implicit val datasetCharacteristicsPayloadFormat: Format[DatasetCharacteristics] = Json.format[DatasetCharacteristics]
}
