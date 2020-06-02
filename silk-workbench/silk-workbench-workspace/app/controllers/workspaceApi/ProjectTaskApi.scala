package controllers.workspaceApi

import controllers.core.util.ControllerUtilsTrait
import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.util.TextSearchUtils
import controllers.workspaceApi.projectTask.{RelatedItem, RelatedItems, TaskCloneRequest, TaskCloneResponse}
import controllers.workspaceApi.search.ItemType
import javax.inject.Inject
import org.silkframework.config.Prefixes
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.validation.BadUserInputException
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}

import scala.util.Try

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

  /** Returns a list of all relevant UI links for a task. */
  def itemLinks(projectId: String, taskId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val task = anyTask(projectId, taskId)
    val itemType = ItemType.itemType(task)
    val itemLinks = ItemType.itemTypeLinks(itemType, projectId, task.id)
    Ok(Json.toJson(itemLinks))
  }

  /** Clones an existing task in the project. */
  def cloneTask(projectId: String, taskId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request =>implicit userContext =>
    validateJson[TaskCloneRequest] { request =>
      val label = request.metaData.label.trim
      if(label == "") {
        throw BadUserInputException("The label must not be empty!")
      }
      val generatedId = IdentifierUtils.generateProjectId(label)
      val (project, fromTask) = projectAndAnyTask(projectId, taskId)
      // Clone task spec, since task specs may contain state, e.g. RDF file dataset
      implicit val resourceManager: ResourceManager = project.resources
      implicit val prefixes: Prefixes = project.config.prefixes
      val clonedTaskSpec = Try(fromTask.data.withProperties(Map.empty)).getOrElse(fromTask.data)
      project.addAnyTask(generatedId, clonedTaskSpec, request.metaData.asMetaData)
      val itemType = ItemType.itemType(fromTask)
      val taskLink = ItemType.itemTypeLinks(itemType, projectId, generatedId).headOption.map(_.path)
      Created(Json.toJson(TaskCloneResponse(generatedId, taskLink)))
    }
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
