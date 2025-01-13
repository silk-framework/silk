package org.silkframework.serialization.json

import io.swagger.v3.oas.annotations.media.Schema
import org.silkframework.rule.vocab.GenericInfo
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.util.Uri
import play.api.libs.json.{Format, JsValue, Json}

case class GenericInfoJson(
  @Schema(description = "The URI or prefixed name of the class")
  uri: String,
  label: Option[String] = None,
  description: Option[String] = None,
  altLabels: Seq[String] = Seq.empty
)

object GenericInfoJson extends JsonCompanion[GenericInfo, GenericInfoJson] {
  override implicit lazy val jsonFormat: Format[GenericInfoJson] = Json.format[GenericInfoJson]

  override def read(json: GenericInfoJson)(implicit readContext: ReadContext): GenericInfo = {
    GenericInfo(
      uri = Uri.parse(json.uri),
      label = json.label,
      description = json.description,
      altLabels = json.altLabels
    )
  }

  override def write(data: GenericInfo)(implicit writeContext: WriteContext[JsValue]): GenericInfoJson = {
    GenericInfoJson(
      uri = writeContext.prefixes.shorten(data.uri),
      label = data.label,
      description = data.description,
      altLabels = data.altLabels
    )
  }
}