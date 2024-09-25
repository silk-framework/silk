package controllers.workspaceApi.projectTask

import controllers.workspaceApi.search.ItemLink
import org.silkframework.serialization.json.MetaDataSerializers.FullTag
import play.api.libs.json.{Format, Json}

/**
  * A related item of a specific task.
  */
case class RelatedItem(id: String,
                       label: String,
                       description: Option[String],
                       `type`: String,
                       itemLinks: Seq[ItemLink],
                       pluginLabel: String,
                       tags: Set[FullTag],
                       searchTags: Seq[String],
                       pluginId: Option[String],
                       projectId: Option[String],
                       readOnly: Option[Boolean])

object RelatedItem {
  implicit val relatedItemFormat: Format[RelatedItem] = Json.format[RelatedItem]
}

case class RelatedItems(total: Int, items: Seq[RelatedItem])

object RelatedItems {
  implicit val relatedItemsFormat: Format[RelatedItems] = Json.format[RelatedItems]
}