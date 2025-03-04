package org.silkframework.serialization.json

import io.swagger.v3.oas.annotations.media.Schema
import org.silkframework.rule.vocab.GenericInfo
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.util.Uri
import play.api.libs.json.{Format, JsValue, Json}

@Schema(description = "Information about a schema element, such as a class or property.")
case class GenericInfoJson(
  @Schema(description = "The URI or prefixed name of this schema element")
  uri: String,
  @Schema(description = "The label of this schema element")
  label: Option[String] = None,
  @Schema(description = "The description of this schema element")
  description: Option[String] = None,
  @Schema(description = "Alternative labels of this schema element")
  altLabels: Option[Seq[String]] = None,
  @Schema(description = "The URI of the vocabulary where this element is defined")
  vocabularyUri: Option[String] = None
)

object GenericInfoJson extends JsonCompanion[GenericInfo, GenericInfoJson] {
  override implicit lazy val jsonFormat: Format[GenericInfoJson] = Json.format[GenericInfoJson]

  override def read(json: GenericInfoJson)(implicit readContext: ReadContext): GenericInfo = {
    GenericInfo(
      uri = Uri.parse(json.uri, readContext.prefixes),
      label = json.label,
      description = json.description,
      altLabels = json.altLabels.getOrElse(Seq.empty),
      vocabularyUri = json.vocabularyUri
    )
  }

  override def write(data: GenericInfo)(implicit writeContext: WriteContext[JsValue]): GenericInfoJson = {
    GenericInfoJson(
      uri = writeContext.prefixes.shorten(data.uri),
      label = data.label,
      description = data.description,
      altLabels = Some(data.altLabels),
      vocabularyUri = data.vocabularyUri
    )
  }
}