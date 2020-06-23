package controllers.projectApi

import controllers.core.util.ControllerUtilsTrait
import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.workspace.JsonSerializer
import controllers.workspaceApi.IdentifierUtils
import controllers.workspaceApi.project.ProjectApiRestPayloads.{ItemMetaData, ProjectCreationData}
import controllers.workspaceApi.project.ProjectLoadingErrors.ProjectTaskLoadingErrorResponse
import controllers.workspaceApi.projectTask.{ItemCloneRequest, ItemCloneResponse}
import controllers.workspaceApi.search.ItemType
import javax.inject.Inject
import org.silkframework.config.{MetaData, Prefixes}
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.serialization.json.JsonSerializers
import org.silkframework.serialization.json.JsonSerializers.MetaDataJsonFormat
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import org.silkframework.workspace.ProjectConfig
import org.silkframework.workspace.io.WorkspaceIO
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Accepting, Action, AnyContent, InjectedController}

import scala.util.Try

/**
  * REST API for project artifacts.
  */
class ProjectApi @Inject()(accessMonitor: WorkbenchAccessMonitor) extends InjectedController with ControllerUtilsTrait {
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
        val generatedId = IdentifierUtils.generateProjectId(label)
        val project = workspace.createProject(ProjectConfig(generatedId, metaData = cleanUpMetaData(metaData).asNewMetaData))
        Created(JsonSerializer.projectJson(project)).
            withHeaders(LOCATION -> s"/api/workspace/projects/$generatedId")
      }
  }

  /** Clone a project (resources and tasks) based on new meta data. */
  def cloneProject(fromProjectId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[ItemCloneRequest] { request =>
      val label = request.metaData.label.trim
      if(label == "") {
        throw BadUserInputException("The label must not be empty!")
      }
      val generatedId = IdentifierUtils.generateProjectId(label)
      val project = getProject(fromProjectId)
      val clonedProjectConfig = project.config.copy(id = generatedId, metaData = request.metaData.asMetaData)
      val clonedProject = workspace.createProject(clonedProjectConfig.copy(projectResourceUriOpt = Some(clonedProjectConfig.generateDefaultUri)))
      WorkspaceIO.copyResources(project.resources, clonedProject.resources)
      // Clone task spec, since task specs may contain state, e.g. RDF file dataset
      implicit val resourceManager: ResourceManager = project.resources
      implicit val prefixes: Prefixes = project.config.prefixes
      for (task <- project.allTasks) {
        val clonedTaskSpec = Try(task.data.withProperties(Map.empty)).getOrElse(task.data)
        clonedProject.addAnyTask(task.id, clonedTaskSpec, task.metaData.asNewMetaData)
      }
      val projectLink = ItemType.itemDetailsPage(ItemType.project, generatedId, generatedId).path
      Created(Json.toJson(ItemCloneResponse(generatedId, projectLink)))
    }
  }

  private def cleanUpMetaData(metaData: MetaData) = {
    MetaData(metaData.label.trim, metaData.description.filter(_.trim.nonEmpty))
  }

  /** Update the project meta data. */
  def updateProjectMetaData(projectId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[ItemMetaData] { newMetaData =>
      val cleanedNewMetaData = newMetaData
      val oldProjectMetaData = workspace.project(projectId).config.metaData
      val mergedMetaData = oldProjectMetaData.copy(label = cleanedNewMetaData.label, description = cleanedNewMetaData.description)
      val updatedMetaData = workspace.updateProjectMetaData(projectId, mergedMetaData.asUpdatedMetaData)
      Ok(JsonSerializers.toJson(updatedMetaData))
    }
  }

  /** Fetches the meta data of a project. */
  def getProjectMetaData(projectId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(JsonSerializers.toJson(getProject(projectId).config.metaData))
  }

  /** Returns all project prefixes */
  def fetchProjectPrefixes(projectId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = getProject(projectId)
    accessMonitor.saveProjectAccess(project.config.id) // Only accessed on the project details page
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
