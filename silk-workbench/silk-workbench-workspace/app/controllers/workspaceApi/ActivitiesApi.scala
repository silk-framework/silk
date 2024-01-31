package controllers.workspaceApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspaceApi.doc.ActivitiesApiDoc
import controllers.workspaceApi.search.SearchApiModel.FacetedSearchResult
import controllers.workspaceApi.search.activity.ActivitySearchRequest
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.runtime.activity.{Status => ActivityStatus}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.ActivitySerializers.ExtendedStatusJsonFormat
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

/**
  * Activities API.
  */
@Tag(name = "Activities")
class ActivitiesApi @Inject() (implicit accessMonitor: WorkbenchAccessMonitor) extends InjectedController with UserContextActions with ControllerUtilsTrait {

  /** List status of all task activities. */
  @Operation(
    summary = "Task activities status",
    description = "Returns status information of a set of task activities. By default all task activities are returned.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(ActivitiesApiDoc.activitiesExample)))
        )
      )
    ))
  def taskActivitiesStatus(@Parameter(
                             name = "projectId",
                             description = "If defined only task activities of a specific project are considered.",
                             required = false,
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[String])
                           )
                           projectId: Option[String],
                           @Parameter(
                             name = "statusFilter",
                             description = "If defined only task activities with a specific status are returned. Valid values are \"Idle\", \"Not executed\", \"Finished\", \"Cancelled\", \"Failed\", \"Successful\", \"Canceling\", \"Running\" and \"Waiting\". States \"Idle\" and \"Not executed\" are synonyms and \"Idle\" is kept only for backwards compatibility. State \"Finished\" is a union of following sub-states \"Cancelled\", \"Failed\" and \"Successful\". \"Waiting\" is the state of an activity being scheduled, but still waiting in queue for being executed.",
                             required = false,
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[String])
                           )
                           statusFilter: Option[String]): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val projects = projectId match {
      case Some(id) =>
        Seq(workspace.project(id))
      case None =>
        workspace.projects
    }
    def statusMatches(status: ActivityStatus, statusFilter: Option[String]): Boolean = {
      statusFilter.isEmpty || statusFilter.contains(status.name) || statusFilter.contains(status.concreteStatus)
    }
    implicit val writeContext: WriteContext[JsValue] = WriteContext.empty
    val activityStatuses =
      for(project <- projects;
          task <- project.allTasks;
          activity <- task.activities;
          status <- activity.status.get
          if statusMatches(status, statusFilter)) yield {
        val extendedJsonFormat = new ExtendedStatusJsonFormat(project.config.id, task.id, activity.name, activity.label, activity.queueTime, activity.startTime)
        extendedJsonFormat.write(status)
      }

    Ok(Json.toJson(activityStatuses))
  }

  /** Faceted search API for the activity search */
  @Operation(
    summary = "Activity search",
    description = "Allows to search over all activities with text search and filter facets.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Search result",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[FacetedSearchResult])
        ))
      )
    )
  )
  @RequestBody(
    content = Array(new Content(
      mediaType = "application/json",
      schema = new Schema(implementation = classOf[ActivitySearchRequest])
    )))
  def activitySearch(): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[ActivitySearchRequest] { searchResult =>
      Ok(Json.toJson(searchResult()))
    }
  }
}



