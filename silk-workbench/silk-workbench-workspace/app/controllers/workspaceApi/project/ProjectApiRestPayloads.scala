package controllers.workspaceApi.project

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.config.MetaData
import org.silkframework.util.Uri
import play.api.libs.json.{Format, Json}

/**
  * REST request and response classes.
  */
object ProjectApiRestPayloads {

  /**
    * Meta data used for projects and tasks. Does not include fields that should not be modified by a user request.
    *
    * @param label       label of the item
    * @param description optional description of the item
    */
  case class ItemMetaData(label: String, description: Option[String] = None,
                          @ArraySchema(schema = new Schema(implementation = classOf[String], required = false, nullable = true))
                          tags: Option[Set[String]] = None) {
    def asMetaData: MetaData = MetaData(Some(label), description, tags = tags.getOrElse(Set.empty).map(new Uri(_)))
  }

  object ItemMetaData {
    implicit val itemMetaDataFormat: Format[ItemMetaData] = Json.format[ItemMetaData]
  }

  /** Data to create a project. */
  case class ProjectCreationData(metaData: ItemMetaData, id:Option[String])

  object ProjectCreationData {
    implicit val projectCreationData: Format[ProjectCreationData] = Json.format[ProjectCreationData]
  }
}