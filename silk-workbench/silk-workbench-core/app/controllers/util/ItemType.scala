package controllers.util

import config.WorkbenchConfig
import org.silkframework.config.{CustomTask, Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.libs.json._

/** The item types the search can be restricted to. */
sealed abstract class ItemType(val id: String, val label: String)

object ItemType {
  case object global extends ItemType("global", "Global")
  case object project extends ItemType("project", "Project")
  case object dataset extends ItemType("dataset", "Dataset")
  case object transform extends ItemType("transform", "Transform")
  case object linking extends ItemType("linking", "Linking")
  case object workflow extends ItemType("workflow", "Workflow")
  case object task extends ItemType("task", "Task")

  val taskTypes: Seq[ItemType] = Seq(workflow, dataset, transform, linking, task)
  val all: Seq[ItemType] = Seq(global, project) ++ taskTypes

  val idToItemType: Map[String, ItemType] = all.map(it => (it.id, it)).toMap

  private def context: String = WorkbenchConfig.applicationContext

  private def workspaceProjectPath(projectId: String) = s"workbench/projects/$projectId"

  /** Link to the item details page. */
  def itemDetailsPage(itemType: ItemType, projectId: String, itemId: String): ItemLink = {
    val detailsPageBase = s"$context/${workspaceProjectPath(projectId)}"
    itemType match {
      case ItemType.dataset =>
        ItemLink("details", "Dataset details page", s"$detailsPageBase/${ItemType.dataset.id}/$itemId")
      case ItemType.transform =>
        ItemLink("details", "Transform details page", s"$detailsPageBase/${ItemType.transform.id}/$itemId")
      case ItemType.linking =>
        ItemLink("details", "Linking details page", s"$detailsPageBase/${ItemType.linking.id}/$itemId")
      case ItemType.workflow =>
        ItemLink("details", "Workflow details page", s"$detailsPageBase/${ItemType.workflow.id}/$itemId")
      case ItemType.task =>
        ItemLink("details", "Task details page", s"$detailsPageBase/${ItemType.task.id}/$itemId")
      case ItemType.project =>
        ItemLink("details", "Project details page", s"$context/${workspaceProjectPath(itemId)}" + WorkbenchConfig.defaultProjectPageSuffix.getOrElse(""))
      case _ =>
        throw new IllegalArgumentException(s"Unsupported item type: $itemType")

    }
  }

  /** All links for a specific item type */
  def itemTypeLinks(itemType: ItemType, projectId: String, itemId: String, taskSpec: Option[TaskSpec]): Seq[ItemLink] = {
    val itemTypeSpecificLinks = itemType match {
      case ItemType.linking => Seq(
      )
      case ItemType.workflow if !WorkbenchConfig().tabs.legacyWorkflowEditor => Seq(
      )
      case ItemType.workflow => Seq(
        ItemLink("editor", "Workflow editor (legacy)", s"$context/workflow/editor/$projectId/$itemId"),
      )
      case _: ItemType if taskSpec.isDefined =>
        taskSpec.get.taskLinks.map(taskLink => ItemLink(taskLink.id, taskLink.id, taskLink.url))
      case _ => Seq()
    }
    Seq(itemDetailsPage(itemType, projectId, itemId)) ++ itemTypeSpecificLinks
  }

  // Convenience method for the above version
  def itemTypeLinks(projectId: String, task: Task[_ <: TaskSpec]): Seq[ItemLink] = {
    itemTypeLinks(itemType(task.data), projectId, task.id, Some(task.data))
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
          case None => throw BadUserInputException(s"Invalid value for itemType. Got '$value'. Value values: " + ItemType.all.map(_.id).mkString(", "))
        }
        case _ => throw BadUserInputException("Invalid value for itemType. String value expected.")
      }
    }
    override def writes(o: ItemType): JsValue = JsString(o.id)
  }
}

case class ItemLink(id: String, label: String, path: String)

object ItemLink {
  implicit val itemLinkFormat: Format[ItemLink] = Json.format[ItemLink]
}
