package controllers.workspaceApi.uriPattern

import play.api.libs.json.{Format, Json}

/** Request for finding URI patterns. */
case class UriPatternRequest(targetClassUris: Seq[String])

object UriPatternRequest {
  implicit val uriPatternRequestFormat: Format[UriPatternRequest] = Json.format[UriPatternRequest]
}
