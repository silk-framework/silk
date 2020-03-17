package controllers.workspaceApi

import java.util.UUID

import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.core.util.ControllerUtilsTrait
import controllers.workspace.JsonSerializer
import controllers.workspaceApi.project.ProjectApiRestPayloads.{ItemMetaData, ProjectCreationData}
import controllers.workspaceApi.project.ProjectLoadingErrors.{ProjectTaskLoadingErrorResponse, Stacktrace}
import javax.inject.Inject
import org.silkframework.config.{MetaData, Prefixes}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.Identifier
import org.silkframework.workspace.ProjectConfig
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Accepting, Action, AnyContent, InjectedController, Result}

/**
  * REST API for project artifacts.
  */
class ProjectApi @Inject()() extends InjectedController with ControllerUtilsTrait {
  private val MARKDOWN_MIME = "text/markdown"
  private val AcceptsMarkdown = Accepting(MARKDOWN_MIME)

  /** Create a project given the meta data. Automatically generates an ID. */
  def createNewProject(): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
      validateJson[ProjectCreationData] { projectCreationData =>
        val metaData = projectCreationData.metaData.asMetaData
        val label = metaData.label.trim
        if(label == "") {
          throw BadUserInputException("The label must not be empty!")
        }
        val generatedId = generateProjectId(label)
        val project = workspace.createProject(ProjectConfig(generatedId, metaData = cleanUpMetaData(metaData)))
        Created(JsonSerializer.projectJson(project)).
            withHeaders(LOCATION -> s"/api/workspace/projects/$generatedId")
      }
  }

  private def cleanUpMetaData(metaData: MetaData) = {
    MetaData(metaData.label.trim, metaData.description.filter(_.trim.nonEmpty))
  }

  /** Update the project meta data. */
  def updateProjectMetaData(projectId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[ItemMetaData] { newMetaData =>
      workspace.updateProjectMetaData(projectId, cleanUpMetaData(newMetaData.asMetaData))
      NoContent
    }
  }

  private def generateProjectId(label: String)
                               (implicit userContext: UserContext): Identifier = {
    val defaultSuffix = "project"
    if(Identifier.fromAllowed(label, alternative = Some(defaultSuffix)) == Identifier(defaultSuffix)) {
      Identifier.fromAllowed(UUID.randomUUID().toString + "_" + defaultSuffix)
    } else {
      Identifier.fromAllowed(UUID.randomUUID().toString + "_" + label)
    }
  }

  /** Returns all project prefixes */
  def fetchProjectPrefixes(projectId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = getProject(projectId)
    Ok(Json.toJson(project.config.prefixes.prefixMap))
  }

  /** Add or update project prefix. */
  def addProjectPrefix(projectId: String, prefixName: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    val project = getProject(projectId)
    validateJson[String] { prefixUri =>
      val newPrefixes = Prefixes(project.config.prefixes.prefixMap ++ Map(prefixName -> prefixUri))
      project.config = project.config.copy(prefixes = newPrefixes)
      Ok(Json.toJson(newPrefixes.prefixMap))
    }
  }

  /** Delete a project prefix */
  def deleteProjectPrefix(projectId: String, prefixName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = getProject(projectId)
    val newPrefixes = Prefixes(project.config.prefixes.prefixMap - prefixName)
    project.config = project.config.copy(prefixes = newPrefixes)
    Ok(Json.toJson(newPrefixes.prefixMap))
  }

  /** Get an error report for tasks that failed loading. */
  def projectTaskLoadingErrorReport(projectId: String, taskId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = getProject(projectId)
    project.loadingErrors.find(_.id == taskId) match {
      case Some(loadingError) =>
        val failedTask = ProjectTaskLoadingErrorResponse.fromTaskLoadingError(loadingError)
        val taskLabel = failedTask.taskLabel.getOrElse(failedTask.taskId)
        render {
          case AcceptsMarkdown() =>
            val markdownHeader = s"""# Project task loading error report
                                    |
                                    |Task '$taskLabel' in project '${project.config.metaData.label}' has failed loading.
                                    |
                                    |""".stripMargin
            Ok(markdownHeader + failedTask.asMarkdown(None)).withHeaders("Content-type" -> MARKDOWN_MIME)
          case Accepts.Json() => // default is JSON
            Ok(Json.toJson(failedTask))
        }
      case None =>
        NotFound
    }
  }

  /** Get an error report for tasks that failed loading. */
  def projectTasksLoadingErrorReport(projectId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = getProject(projectId)
    val failedTasks = project.loadingErrors.map(ProjectTaskLoadingErrorResponse.fromTaskLoadingError)
    render {
      case AcceptsMarkdown() =>
        val sb = new StringBuilder()
        val markdownHeader = s"""# Project Tasks Loading Error Report
                               |
                               |In project '${project.config.metaData.label}' ${failedTasks.size} task/s could not be loaded.
                               |""".stripMargin
        sb.append(markdownHeader)
        for((failedTask, idx) <- failedTasks.zipWithIndex) {
          sb.append("\n").append(failedTask.asMarkdown(Some(idx + 1)))
        }
        Ok(sb.toString()).withHeaders("Content-type" -> MARKDOWN_MIME)
      case Accepts.Json() => // default is JSON
        Ok(Json.toJson(failedTasks))
    }
  }
}