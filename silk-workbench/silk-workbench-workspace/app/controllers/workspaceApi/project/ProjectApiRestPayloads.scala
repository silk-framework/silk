package controllers.workspaceApi.project

import org.silkframework.config.MetaData
import play.api.libs.json.{Format, Json}

/**
  * REST request and response classes.
  */
object ProjectApiRestPayloads {

  /**
    * Meta data used for projects and tasks.
    *
    * @param label       label of the item
    * @param description optional description of the item
    */
  case class ItemMetaData(label: String, description: Option[String] = None) {
    def asMetaData: MetaData = MetaData(label, description)
  }

  object ItemMetaData {
    implicit val itemMetaDataFormat: Format[ItemMetaData] = Json.format[ItemMetaData]
  }

  /** Data to create a project. */
  case class ProjectCreationData(metaData: ItemMetaData)

  object ProjectCreationData {
    implicit val projectCreationData: Format[ProjectCreationData] = Json.format[ProjectCreationData]
  }
}