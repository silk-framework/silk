package controllers.workspace

import controllers.core.util.ControllerUtilsTrait
import org.silkframework.config.TaskSpec
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerializers.{TaskJsonFormat, TaskSpecJsonFormat}
import org.silkframework.workspace.{ProjectTask, User}
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{Action, BodyParsers, Controller}

/**
  * API to search for tasks in the workspace.
  */
class SearchApi extends Controller with ControllerUtilsTrait {

  def search() = Action(BodyParsers.parse.json) { implicit request =>
    implicit val responseOptionsReader = Json.reads[ResponseFormat]
    implicit val searchRequestReader = Json.reads[SearchRequest]
    validateJson[SearchRequest] { searchRequest =>
      Ok(searchRequest())
    }
  }

  case class SearchRequest(project: Option[String], searchTerm: Option[String], response: Option[ResponseFormat]) {

    private val responseFormat = response.getOrElse(ResponseFormat())

    /**
      * Executes the search request and generates the JSON response.
      */
    def apply(): JsValue = {
      var tasks = projects.flatMap(_.allTasks)

      for(term <- searchTerm) {
        val lowerCaseTerm = term.toLowerCase
        tasks = tasks.filter(task => matchesSearchTerm(lowerCaseTerm, task))
      }

      JsArray(tasks.map(responseFormat.writeTask))
    }

    /**
      * Retrieves all projects that are selected by the request.
      */
    private def projects = {
      project match {
        case Some(projectName) =>
          Seq(User().workspace.project(projectName))
        case None =>
          User().workspace.projects
      }
    }

    /**
      * Checks if a task matches the search term.
      */
    private def matchesSearchTerm(lowerCaseSearchTerm: String, task: ProjectTask[_ <: TaskSpec]): Boolean = {
      val idMatch = task.id.toLowerCase.contains(lowerCaseSearchTerm)
      val propertiesMatch = task.data.properties(task.project.config.prefixes).exists(_._2.toLowerCase.contains(lowerCaseSearchTerm))
      idMatch || propertiesMatch
    }
  }

  case class ResponseFormat(includeMetaData: Option[Boolean] = None,
                            includeTaskProperties: Option[Boolean] = None,
                            includeTaskData: Option[Boolean] = None) {

    // JSON format to serialize tasks according to the options
    private val taskFormat: TaskJsonFormat[TaskSpec] = {
      new TaskJsonFormat(includeMetaData = includeMetaData.getOrElse(true),
        includeTaskProperties = includeTaskProperties.getOrElse(true),
        includeTaskData = includeTaskData.getOrElse(false))(TaskSpecJsonFormat)
    }

    def writeTask(task: ProjectTask[_ <: TaskSpec]): JsValue = {
      taskFormat.write(task)(WriteContext[JsValue](prefixes = task.project.config.prefixes, projectId = Some(task.project.name)))
    }
  }
}


