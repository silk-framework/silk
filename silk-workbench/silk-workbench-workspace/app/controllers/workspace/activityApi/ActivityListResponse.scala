package controllers.workspace.activityApi

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import play.api.libs.json.{Json, OWrites}

object ActivityListResponse {

  implicit val activityInstanceFormat: OWrites[ActivityInstance] = Json.writes[ActivityInstance]

  implicit val activityListEntryFormat: OWrites[ActivityListEntry] = Json.writes[ActivityListEntry]

  @Schema(description = "An activity and all of its instances. Non-singleton activities may have multiple parallel instances while singleton instances always have one instance.")
  case class ActivityListEntry(@Schema(description = "The name of the activity.")
                               name: String,
                               @ArraySchema(schema = new Schema(implementation = classOf[ActivityInstance]))
                               instances: Seq[ActivityInstance])

  @Schema(description = "An activity instance.")
  case class ActivityInstance(@Schema(description = "The identifier of the activity instance.")
                              id: String)

}
