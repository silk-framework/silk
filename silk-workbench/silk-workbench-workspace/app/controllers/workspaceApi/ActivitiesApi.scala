package controllers.workspaceApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
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
            examples = Array(new ExampleObject(ActivitiesApi.activitiesExample)))
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

object ActivitiesApi {

  private final val activitiesExample =
"""
[
    {
        "activity": "TypesCache",
        "cancelled": false,
        "concreteStatus": "Successful",
        "exceptionMessage": null,
        "failed": false,
        "isRunning": false,
        "lastUpdateTime": 1595861385992,
        "message": "Finished in 46ms",
        "progress": 100,
        "project": "singleProject",
        "runtime": 46,
        "startTime": 1595861385946,
        "statusName": "Finished",
        "task": "d57c393f-8f3f-48ba-ba13-8e815e04d557_CSVdataset"
    },
    {
        "activity": "ExecuteTransform",
        "concreteStatus": "Not executed",
        "failed": false,
        "isRunning": false,
        "lastUpdateTime": 1595861385941,
        "message": "Idle",
        "progress": null,
        "project": "singleProject",
        "startTime": null,
        "statusName": "Idle",
        "task": "a0d18ae0-085b-4a06-aee1-b4c19bd00eac_failTransform"
    },
    {
        "activity": "ExecuteLocalWorkflow",
        "cancelled": false,
        "concreteStatus": "Failed",
        "exceptionMessage": "Exception during execution of workflow operator a0d18ae0-085b-4a06-aee1-b4c19bd00eac_failTransform. Cause: No input given to transform specification executor a0d18ae0-085b-4a06-aee1-b4c19bd00eac_failTransform!",
        "failed": true,
        "isRunning": false,
        "lastUpdateTime": 1595861468748,
        "message": "Failed after 135ms: Exception during execution of workflow operator a0d18ae0-085b-4a06-aee1-b4c19bd00eac_failTransform. Cause: No input given to transform specification executor a0d18ae0-085b-4a06-aee1-b4c19bd00eac_failTransform!",
        "progress": 100,
        "project": "singleProject",
        "runtime": 135,
        "startTime": 1595861468613,
        "statusName": "Finished",
        "task": "e7dc14e5-b45b-4dc5-9933-bbc2750630f5_failedWorkflow"
    }
]
"""

}

