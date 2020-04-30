package controllers.workspaceApi.search

import config.WorkbenchConfig
import controllers.workspaceApi.search.SearchApiModel.PROJECT_TYPE
import org.silkframework.config.{CustomTask, Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.libs.json.{Format, JsResult, JsString, JsSuccess, JsValue, Json, Writes}

/** The item types the search can be restricted to. */
sealed abstract class ItemType(val id: String, val label: String)

object ItemType {
  case object project extends ItemType(PROJECT_TYPE, "Project")
  case object dataset extends ItemType("dataset", "Dataset")
  case object transform extends ItemType("transform", "Transform")
  case object linking extends ItemType("linking", "Linking")
  case object workflow extends ItemType("workflow", "Workflow")
  case object task extends ItemType("task", "Task")

  val ordered: Seq[ItemType] = Seq(project, workflow, dataset, transform, linking, task)
  val idToItemType: Map[String, ItemType] = ordered.map(it => (it.id, it)).toMap

  private def context: String = WorkbenchConfig.applicationContext

  // TODO: Update URL after deciding on path for new workspace
  private def workspaceProjectPath(projectId: String) = s"workspaceNew/projects/$projectId"

  /** Link to the item details page. */
  def itemDetailsPage(itemType: ItemType, projectId: String, itemId: String): ItemLink = {
    val detailsPageBase = s"$context/${workspaceProjectPath(projectId)}"
    itemType match {
      case ItemType.dataset =>
        ItemLink("Dataset details page", s"$detailsPageBase/${ItemType.dataset.id}/$itemId")
      case ItemType.transform =>
        ItemLink("Transform details page", s"$detailsPageBase/${ItemType.transform.id}/$itemId")
      case ItemType.linking =>
        ItemLink("Linking details page", s"$detailsPageBase/${ItemType.linking.id}/$itemId")
      case ItemType.workflow =>
        ItemLink("Workflow details page", s"$detailsPageBase/${ItemType.workflow.id}/$itemId")
      case ItemType.task =>
        ItemLink("Task details page", s"$detailsPageBase/${ItemType.task.id}/$itemId")
      case ItemType.project =>
        ItemLink("Project details page", s"$context/${workspaceProjectPath(itemId)}")
    }
  }

  /** All links for a specific item type */
  def itemTypeLinks(itemType: ItemType, projectId: String, itemId: String): Seq[ItemLink] = {
    val itemTypeSpecificLinks = itemType match {
      case ItemType.transform => Seq(
        ItemLink("Mapping editor", s"$context/transform/$projectId/$itemId/editor"),
        ItemLink("Transform evaluation", s"$context/transform/$projectId/$itemId/evaluate"),
        ItemLink("Transform execution", s"$context/transform/$projectId/$itemId/execute")
      )
      case ItemType.linking => Seq(
        ItemLink("Linking editor", s"$context/linking/$projectId/$itemId/editor"),
        ItemLink("Linking evaluation", s"$context/linking/$projectId/$itemId/evaluate"),
        ItemLink("Linking execution", s"$context/linking/$projectId/$itemId/execute")
      )
      case ItemType.workflow => Seq(
        ItemLink("Workflow editor", s"$context/workflow/editor/$projectId/$itemId")
      )
      case _ => Seq()
    }
    Seq(itemDetailsPage(itemType, projectId, itemId)) ++ itemTypeSpecificLinks
  }

  // Convenience method for the above version
  def itemTypeLinks(projectId: String, task: Task[_ <: TaskSpec]): Seq[ItemLink] = {
    itemTypeLinks(itemType(task.data), projectId, task.id)
  }

  /** Returns the item type of a specific task. Throws exception if the task is not assigned to any item type.*/
  def itemType(task: TaskSpec): ItemType = {
    task match {
      case _: TransformSpec => transform
      case _: LinkSpec => linking
      case _: Workflow => workflow
      case _: DatasetSpec[_] => dataset
      case _: CustomTask => ItemType.task
      case _ => throw new IllegalArgumentException(s"No known item type for task class '${task.getClass.getSimpleName}'.")
    }
  }

  implicit val itemTypeFormat: Format[ItemType] = new Format[ItemType] {
    override def reads(json: JsValue): JsResult[ItemType] = {
      json match {
        case JsString(value) =>ItemType.idToItemType.get(value) match {
          case Some(itemType) => JsSuccess(itemType)
          case None => throw BadUserInputException(s"Invalid value for itemType. Got '$value'. Value values: " + ItemType.ordered.map(_.id).mkString(", "))
        }
        case _ => throw BadUserInputException("Invalid value for itemType. String value expected.")
      }
    }
    override def writes(o: ItemType): JsValue = JsString(o.id)
  }
}

case class ItemLink(label: String, path: String)

object ItemLink {
  implicit val itemLinkFormat: Format[ItemLink] = Json.format[ItemLink]
}