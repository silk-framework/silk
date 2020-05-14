package controllers.workspace

import controllers.core.RequestUserContextAction
import controllers.core.util.ControllerUtilsTrait
import javax.inject.Inject
import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.{JsonFormat, JsonSerializers}
import org.silkframework.serialization.json.JsonSerializers.{TaskFormatOptions, TaskJsonFormat, TaskSpecJsonFormat}
import org.silkframework.workspace.{ProjectTask, WorkspaceFactory}
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{Action, BodyParsers, InjectedController}

/**
  * API to search for tasks in the workspace.
  */
class SearchApi @Inject() () extends InjectedController with ControllerUtilsTrait {


  def search(): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    implicit val responseOptionsReader = Json.reads[TaskFormatOptions]
    implicit val searchRequestReader = Json.reads[SearchRequest]
    validateJson[SearchRequest] { searchRequest =>
      Ok(searchRequest())
    }
  }

  case class SearchRequest(project: Option[String], searchTerm: Option[String], formatOptions: Option[TaskFormatOptions]) {

    // JSON format to serialize tasks according to the options
    private def taskFormat(userContext: UserContext): TaskJsonFormat[TaskSpec] = {
      implicit val jsonFormat:JsonFormat[TaskSpec] = TaskSpecJsonFormat
      new TaskJsonFormat(formatOptions.getOrElse(TaskFormatOptions()), Some(userContext))
    }

    /**
      * Executes the search request and generates the JSON response.
      */
    def apply()(implicit userContext: UserContext): JsValue = {
      var tasks = projects.flatMap(_.allTasks)

      for(term <- searchTerm) {
        val lowerCaseTerm = term.toLowerCase
        tasks = tasks.filter(task => matchesSearchTerm(lowerCaseTerm, task))
      }

      JsArray(tasks.map(writeTask))
    }

    /**
      * Retrieves all projects that are selected by the request.
      */
    private def projects(implicit userContext: UserContext) = {
      project match {
        case Some(projectName) =>
          Seq(WorkspaceFactory().workspace.project(projectName))
        case None =>
          WorkspaceFactory().workspace.projects
      }
    }

    def writeTask(task: ProjectTask[_ <: TaskSpec])
                 (implicit userContext: UserContext): JsValue = {
      taskFormat(userContext).write(task)(WriteContext[JsValue](prefixes = task.project.config.prefixes, projectId = Some(task.project.name)))
    }

    /**
      * Checks if a task matches the search term.
      */
    private def matchesSearchTerm(lowerCaseSearchTerm: String, task: ProjectTask[_ <: TaskSpec]): Boolean = {
      val idMatch = task.id.toLowerCase.contains(lowerCaseSearchTerm)
      val labelMatch = task.metaData.label.toLowerCase.contains(lowerCaseSearchTerm)
      val descriptionMatch = task.metaData.description.getOrElse("").toLowerCase.contains(lowerCaseSearchTerm)
      val propertiesMatch = task.data.properties(task.project.config.prefixes).exists(_._2.toLowerCase.contains(lowerCaseSearchTerm))
      idMatch || labelMatch || descriptionMatch || propertiesMatch
    }
  }
}


