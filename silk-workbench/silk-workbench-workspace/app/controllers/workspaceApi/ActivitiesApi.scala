package controllers.workspaceApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspaceApi.doc.ActivitiesApiDoc
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

import javax.inject.Inject
import org.silkframework.runtime.activity.{Status => ActivityStatus}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.ActivitySerializers.ExtendedStatusJsonFormat
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}

/**
  * Activities API.
  */
@Tag(name = "Activities")
class ActivitiesApi @Inject() () extends InjectedController with UserContextActions with ControllerUtilsTrait {

  /** List status of all task activities. */
  @Operation(
    summary = "Task activities status",
    description = "Returns status information of a set of task activities. By default all task activities are returned.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
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
                           statusFilter: Option[String]): Action[AnyContent] = RequestUserContextAction { implicit request =>implicit userContext =>
    val activityStatusTraversable: Traversable[JsValue] = new Traversable[JsValue] {
      override def foreach[U](f: JsValue => U): Unit = {
        val projects = projectId match {
          case Some(id) =>
            Seq(workspace.project(id))
          case None =>
            workspace.projects
        }
        implicit val writeContext: WriteContext[JsValue] = WriteContext()
        for(project <- projects;
            task <- project.allTasks;
            activity <- task.activities;
            status <- activity.status.get
            if statusMatches(status, statusFilter)) {
          val extendedJsonFormat = new ExtendedStatusJsonFormat(project.config.id, task.id, activity.name, activity.startTime)
          f(extendedJsonFormat.write(status))
        }
      }

      def statusMatches(status: ActivityStatus, statusFilter: Option[String]): Boolean = {
        statusFilter.isEmpty || statusFilter.contains(status.name) || statusFilter.contains(status.concreteStatus)
      }
    }
    Ok(Json.toJson(activityStatusTraversable.toSeq))
  }
}



