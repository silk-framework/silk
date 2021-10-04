package controllers.workspace.activityApi

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import play.api.libs.json.{Format, Json}

object ActivityListResponse {

  implicit val activityInstanceFormat: Format[ActivityInstance] = Json.format[ActivityInstance]

  implicit val activityCharacteristicsFormat: Format[ActivityCharacteristics] = Json.format[ActivityCharacteristics]

  implicit val activityMetaDataFormat: Format[ActivityMetaData] = Json.format[ActivityMetaData]

  implicit val activityListEntryFormat: Format[ActivityListEntry] = Json.format[ActivityListEntry]

  @Schema(description = "An activity and all of its instances. Non-singleton activities may have multiple parallel instances while singleton instances always have one instance.")
  case class ActivityListEntry(@Schema(description = "The name of the activity.")
                               name: String,
                               @ArraySchema(schema = new Schema(implementation = classOf[ActivityInstance]))
                               instances: Seq[ActivityInstance],
                               @Schema(implementation = classOf[ActivityCharacteristics])
                               activityCharacteristics: ActivityCharacteristics,
                               @Schema(implementation = classOf[ActivityMetaData])
                               metaData: Option[ActivityMetaData]
                               )

  @Schema(description = "Meta data of an activity.")
  case class ActivityMetaData(@Schema(description = "The project of an activity.")
                              projectId: Option[String],
                              @Schema(description = "The task ID of an activity.")
                              taskId: Option[String])

  @Schema(description = "An activity instance.")
  case class ActivityInstance(@Schema(description = "The identifier of the activity instance.")
                              id: String)

  @Schema(description = "Activity characteristics.")
  case class ActivityCharacteristics(@Schema(description = "If this activity is one of the main activities for the requested object, i.e. workspace, project or task.")
                                     isMainActivity: Boolean,
                                     @Schema(description = "If this activity is a cache activity used to cache values that are potentially expensive to compute.")
                                     isCacheActivity: Boolean)

}
