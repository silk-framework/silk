package controllers.workspaceApi

import controllers.core.UserContextAction
import controllers.core.util.ControllerUtilsTrait
import controllers.util.TextSearchUtils
import controllers.workspaceApi.projectTask.{RelatedItem, RelatedItems}
import controllers.workspaceApi.search.ItemType
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, InjectedController}

/**
  * API for project tasks.
  */
class ProjectTaskApi @Inject()() extends InjectedController with ControllerUtilsTrait {
  /** Fetch all related items (tasks) for a specific project task. */
  def relatedItems(projectId: String, taskId: String, textQuery: Option[String]): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = getProject(projectId)
    val task = project.anyTask(taskId)
    val relatedTasks = (task.data.referencedTasks.toSeq ++ task.findDependentTasks(recursive = false).toSeq ++ task.findRelatedTasksInsideWorkflows.toSeq).distinct.
        flatMap(id => project.anyTaskOption(id))
    val relatedItems = relatedTasks map { task =>
      val itemType = ItemType.itemType(task)
      val itemLinks = ItemType.itemTypeLinks(itemType, projectId, task.id)
      RelatedItem(task.id, task.taskLabel(Int.MaxValue), task.metaData.description, itemType.label, itemLinks)
    }
    val filteredItems = filterRelatedItems(relatedItems, textQuery)
    val total = relatedItems.size
    val result = RelatedItems(total, filteredItems)
    Ok(Json.toJson(result))
  }

  private def filterRelatedItems(relatedItems: Seq[RelatedItem], textQuery: Option[String]): Seq[RelatedItem] = {
    val searchWords =  TextSearchUtils.extractSearchTerms(textQuery.getOrElse(""))
    if(searchWords.isEmpty) {
      relatedItems
    } else {
      relatedItems.filter(relatedItem => // Description is not displayed, so don't search in description.
        TextSearchUtils.matchesSearchTerm(searchWords, s"${relatedItem.label} ${relatedItem.`type`}".toLowerCase))
    }
  }
}
