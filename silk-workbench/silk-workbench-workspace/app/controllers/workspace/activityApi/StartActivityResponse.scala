package controllers.workspace.activityApi

import io.swagger.v3.oas.annotations.media.Schema
import play.api.libs.json.{Format, Json}

@Schema(example = "{ \"activityId\": \"MyActivity\", \"instanceId\": \"MyActivityInstance1\" }")
case class StartActivityResponse(activityId: String, instanceId: String)

object StartActivityResponse {

  implicit val writeFormat: Format[StartActivityResponse] = Json.format[StartActivityResponse]

}
