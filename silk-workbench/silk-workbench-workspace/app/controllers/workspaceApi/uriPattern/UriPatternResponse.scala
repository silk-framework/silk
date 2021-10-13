package controllers.workspaceApi.uriPattern

import play.api.libs.json.{Format, Json}

/** Response for a find URI pattern request. */
case class UriPatternResponse(results: Seq[UriPatternResult])

/** URI pattern candidate. */
case class UriPatternResult(targetClassUri: String,
                            label: Option[String],
                            value: String)

object UriPatternResponse {
  implicit val uriPatternResultFormat: Format[UriPatternResult] = Json.format[UriPatternResult]
  implicit val uriPatternResponseFormat: Format[UriPatternResponse] = Json.format[UriPatternResponse]
}