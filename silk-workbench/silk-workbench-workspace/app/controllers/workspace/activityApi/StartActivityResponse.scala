package controllers.workspace.activityApi

import io.swagger.v3.oas.annotations.media.Schema
import play.api.libs.json.{Json, OWrites}

@Schema(example = "{ \"activityId\": \"MyActivity\" }")
case class StartActivityResponse(activityId: String)

object StartActivityResponse {

  implicit val writeFormat: OWrites[StartActivityResponse] = Json.writes[StartActivityResponse]

}
