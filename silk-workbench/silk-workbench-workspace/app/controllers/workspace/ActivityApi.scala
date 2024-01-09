package controllers.workspace

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Merge, Source}
import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.errorReporting.ErrorReport.ErrorReportItem
import controllers.util.{AkkaUtils, SerializationUtils}
import controllers.workspace.activityApi.ActivityListResponse.ActivityListEntry
import controllers.workspace.activityApi.{ActivityFacade, StartActivityResponse}
import controllers.workspace.doc.ActivityApiDoc
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.TaskSpec
import org.silkframework.execution.report.Stacktrace
import org.silkframework.runtime.activity._
import org.silkframework.runtime.serialization.{Serialization, WriteContext}
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException}
import org.silkframework.serialization.json.ActivitySerializers.ExtendedStatusJsonFormat
import org.silkframework.workbench.utils.ErrorResult
import org.silkframework.workspace.activity.WorkspaceActivity
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc._

import java.util.logging.{LogRecord, Logger}
import javax.inject.Inject
import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}
import scala.language.existentials

@Tag(name = "Activities", description = ActivityApiDoc.activityDoc)
class ActivityApi @Inject() (implicit system: ActorSystem, mat: Materializer) extends InjectedController with UserContextActions with ControllerUtilsTrait {

  @Operation(
    summary = "List activities",
    description = "Lists either global, project or task activities.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          array = new ArraySchema(schema = new Schema(implementation = classOf[ActivityListEntry])),
          examples = Array(new ExampleObject(ActivityApiDoc.activityListExample))
        ))
      )
    ))
  def listActivities(@Parameter(
                       name = "project",
                       description = "Optional project identifier. If not provided or empty, global activities will be listed.",
                       required = false,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[String])
                     )
                     projectName: String,
                     @Parameter(
                       name = "task",
                       description = "Optional task identifier. If not provided or empty, project activities will be listed.",
                       required = false,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[String])
                     )
                     taskName: String,
                     @Parameter(
                       name = "add dependent activities",
                       description = "Optional parameter to also request activities that a task depends on.",
                       required = false,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[Boolean])
                     )
                     addDependentActivities: Boolean
                    ): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    val response = ActivityFacade.listActivities(projectName, taskName, addDependentActivities)
    Ok(Json.toJson(response))
  }

  def globalWorkspaceActivities(): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    val response = ActivityFacade.listActivities("", "")
    Ok(Json.toJson(response))
  }

  def getProjectActivities(projectName: String): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    val response = ActivityFacade.listActivities(projectName, "")
    Ok(Json.toJson(response))
  }

  def getTaskActivities(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    val response = ActivityFacade.listActivities(projectName, taskName)
    Ok(Json.toJson(response))
  }

  @Operation(
    summary = "Start activity (non-blocking)",
    description = "Starts an activity. The call returns immediately without waiting for the activity to complete.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[StartActivityResponse])
        ))
      ),
      new ApiResponse(
        responseCode = "503",
        description = "Activity execution could not be started because concurrent execution limit is reached."
      )
  ))
  @RequestBody(
    description = "Optionally updates configuration parameters, before starting the activity.",
    required = false,
    content = Array(
      new Content(
        mediaType = "application/x-www-form-urlencoded",
        schema = new Schema(implementation = classOf[String]),
        examples = Array(new ExampleObject("param1=value1&param2=value2"))
      )
    )
  )
  def startActivity(@Parameter(
                      name = "project",
                      description = "Optional project identifier. If not provided or empty, global activities will be addressed.",
                      required = false,
                      in = ParameterIn.QUERY,
                      schema = new Schema(implementation = classOf[String])
                    )
                    projectName: String,
                    @Parameter(
                      name = "task",
                      description = "Optional task identifier. If not provided or empty, project activities will be addressed.",
                      required = false,
                      in = ParameterIn.QUERY,
                      schema = new Schema(implementation = classOf[String])
                    )
                    taskName: String,
                    @Parameter(
                      name = "activity",
                      description = "Activity name.",
                      required = true,
                      in = ParameterIn.QUERY,
                      schema = new Schema(implementation = classOf[String])
                    )
                    activityName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext: UserContext =>
    val response = ActivityFacade.start(projectName, taskName, activityName, blocking = false, activityConfig(request))
    Ok(Json.toJson(response))
  }

  @Operation(
    summary = "Start activity (blocking)",
    description = "Starts the activity and returns after it has completed.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[StartActivityResponse])
        ))
      ),
      new ApiResponse(
        responseCode = "503",
        description = "Activity execution could not be started because concurrent execution limit is reached."
      )
    ))
  @RequestBody(
    description = "Optionally updates configuration parameters, before starting the activity.",
    required = false,
    content = Array(
      new Content(
        mediaType = "application/x-www-form-urlencoded",
        schema = new Schema(implementation = classOf[String]),
        examples = Array(new ExampleObject("param1=value1&param2=value2"))
      )
    )
  )
  def startActivityBlocking(@Parameter(
                              name = "project",
                              description = "Optional project identifier. If not provided or empty, global activities will be addressed.",
                              required = false,
                              in = ParameterIn.QUERY,
                              schema = new Schema(implementation = classOf[String])
                            )
                            projectName: String,
                            @Parameter(
                              name = "task",
                              description = "Optional task identifier. If not provided or empty, project activities will be addressed.",
                              required = false,
                              in = ParameterIn.QUERY,
                              schema = new Schema(implementation = classOf[String])
                            )
                            taskName: String,
                            @Parameter(
                              name = "activity",
                              description = "Activity name.",
                              required = true,
                              in = ParameterIn.QUERY,
                              schema = new Schema(implementation = classOf[String])
                            )
                            activityName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext: UserContext =>
    val response = ActivityFacade.start(projectName, taskName, activityName, blocking = true, activityConfig(request))
    Ok(Json.toJson(response))
  }

  @Operation(
    summary = "Start activity prioritized (non-blocking)",
    description = ActivityApiDoc.startPrioritizedDoc,
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[StartActivityResponse])
        ))
      )
    ))
  def startPrioritized(@Parameter(
                         name = "project",
                         description = "Optional project identifier. If not provided or empty, global activities will be addressed.",
                         required = false,
                         in = ParameterIn.QUERY,
                         schema = new Schema(implementation = classOf[String])
                        )
                        projectName: String,
                        @Parameter(
                          name = "task",
                          description = "Optional task identifier. If not provided or empty, project activities will be addressed.",
                          required = false,
                          in = ParameterIn.QUERY,
                          schema = new Schema(implementation = classOf[String])
                        )
                        taskName: String,
                        @Parameter(
                          name = "activity",
                          description = "Activity name.",
                          required = true,
                          in = ParameterIn.QUERY,
                          schema = new Schema(implementation = classOf[String])
                        )
                        activityName: String): Action[AnyContent] = RequestUserContextAction { request =>
    implicit userContext: UserContext =>
      val response = ActivityFacade.startPrioritized(projectName, taskName, activityName)
      Ok(Json.toJson(response))
  }

  @Operation(
    summary = "Cancel activity",
    description = "Requests cancellation of an activity. The call returns immediately and does not wait until the activity has been cancelled.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the cancellation has been requested successfully."
     )
  ))
  def cancelActivity(@Parameter(
                       name = "project",
                       description = "Optional project identifier. If not provided or empty, global activities will be addressed.",
                       required = false,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[String])
                     )
                     projectName: String,
                     @Parameter(
                       name = "task",
                       description = "Optional task identifier. If not provided or empty, project activities will be addressed.",
                       required = false,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[String])
                     )
                     taskName: String,
                     @Parameter(
                       name = "activity",
                       description = "Activity name.",
                       required = true,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[String])
                     )
                     activityName: String,
                     @Parameter(
                       name = "instance",
                       description = ActivityApiDoc.activityInstanceParameterDesc,
                       required = false,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[String])
                     )
                     activityInstance: String): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    val (activity, _) = activityControl(projectName, taskName, activityName, activityInstance)
    activity.cancel()
    Ok
  }

  def restartActivity(projectName: String,
                      taskName: String,
                      activityName: String,
                      activityInstance: String,
                      blocking: Boolean): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    val (activity, _) = activityControl(projectName, taskName, activityName, activityInstance)
    activity.reset()
    if(blocking) {
      activity.startBlocking()
    } else {
      activity.start()
    }
    Ok
  }

  @deprecated(message = "This endpoint does not return the actual config parameters of a given activity instance, but just the default values.")
  def getActivityConfig(projectName: String, taskName: String, activityName: String): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    val workspace = WorkspaceFactory().workspace
    val activityConfig = {
      if(projectName.trim.isEmpty) {
         workspace.activity(activityName).defaultConfig
      } else {
        val project = workspace.project(projectName)
        if (taskName.nonEmpty) {
          val task = project.anyTask(taskName)
          task.activity(activityName).defaultConfig
        } else {
          project.activity(activityName).defaultConfig
        }
      }
    }

    Ok(JsonSerializer.activityConfig(activityConfig))
  }

  @deprecated(message = "Configuration should be attached to a start(blocking) request.")
  def postActivityConfig(projectName: String,
                         taskName: String,
                         activityName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext: UserContext =>
    val config = activityConfig(request)
    if (config.nonEmpty) {
      val workspace = WorkspaceFactory().workspace
      if(projectName.trim.isEmpty) {
        workspace.activity(activityName).update(config)
      } else {
        val project = workspace.project(projectName)
        if (taskName.nonEmpty) {
          val task = project.anyTask(taskName)
          task.activity(activityName).update(config)
        } else {
          project.activity(activityName).update(config)
        }
      }
      Ok
    } else {
      ErrorResult(BadUserInputException("No config supplied"))
    }
  }

  @Operation(
    summary = "Get activity status",
    description = ActivityApiDoc.activityStatusDescription,
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(ActivityApiDoc.activityStatusExample))
        ))
      )
  ))
  def getActivityStatus(@Parameter(
                          name = "project",
                          description = "Optional project identifier. If not provided or empty, global activities will be addressed.",
                          required = false,
                          in = ParameterIn.QUERY,
                          schema = new Schema(implementation = classOf[String])
                        )
                        projectName: String,
                        @Parameter(
                          name = "task",
                          description = "Optional task identifier. If not provided or empty, project activities will be addressed.",
                          required = false,
                          in = ParameterIn.QUERY,
                          schema = new Schema(implementation = classOf[String])
                        )
                        taskName: String,
                        @Parameter(
                          name = "activity",
                          description = "Activity name.",
                          required = true,
                          in = ParameterIn.QUERY,
                          schema = new Schema(implementation = classOf[String])
                        )
                        activityName: String,
                        @Parameter(
                          name = "instance",
                          description = ActivityApiDoc.activityInstanceParameterDesc,
                          required = false,
                          in = ParameterIn.QUERY,
                          schema = new Schema(implementation = classOf[String])
                        )
                        activityInstance: String): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    val (activity, activityLabel) = activityControl(projectName, taskName, activityName, activityInstance)
    implicit val writeContext = WriteContext.empty[JsValue]

    Ok(new ExtendedStatusJsonFormat(projectName, taskName, activityName, activityLabel, activity.startTime).write(activity.status()))
  }

  @Operation(
    summary = "Get activity value",
    description = "Retrieves the current value of this activity, if any.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The activity value. The ACCEPT header controls the serialization format (typically JSON or XML)."
      ),
      new ApiResponse(
        responseCode = "406",
        description = "No serializer is registered for the requested format."
      )
    ))
  def getActivityValue(@Parameter(
                         name = "project",
                         description = "Optional project identifier. If not provided or empty, global activities will be addressed.",
                         required = false,
                         in = ParameterIn.QUERY,
                         schema = new Schema(implementation = classOf[String])
                       )
                       projectName: String,
                       @Parameter(
                         name = "task",
                         description = "Optional task identifier. If not provided or empty, project activities will be addressed.",
                         required = false,
                         in = ParameterIn.QUERY,
                         schema = new Schema(implementation = classOf[String])
                       )
                       taskName: String,
                       @Parameter(
                         name = "activity",
                         description = "Activity name.",
                         required = true,
                         in = ParameterIn.QUERY,
                         schema = new Schema(implementation = classOf[String])
                       )
                       activityName: String,
                       @Parameter(
                         name = "instance",
                         description = ActivityApiDoc.activityInstanceParameterDesc,
                         required = false,
                         in = ParameterIn.QUERY,
                         schema = new Schema(implementation = classOf[String])
                       )
                       activityInstance: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext: UserContext =>
    implicit val project: Option[Project] = if(projectName.trim.isEmpty) {
      None
    } else {
      Some(WorkspaceFactory().workspace.project(projectName))
    }
    val (activity, _) = activityControl(projectName, taskName, activityName, activityInstance)
    activity.value.get match {
      case Some(value) =>
        SerializationUtils.serializeRuntime(value)
      case None =>
        throw NotFoundException(s"No value found for activity.")
    }
  }

  @deprecated
  def recentActivities(maxCount: Int): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    // Get all projects and tasks
    val projects = WorkspaceFactory().workspace.projects
    val tasks: Seq[ProjectTask[_]] = projects.flatMap(_.allTasks)

    // Get all activities
    val projectActivities = projects.flatMap(_.activities)
    val taskActivities = tasks.flatMap(_.activities.asInstanceOf[Seq[WorkspaceActivity[_ <: HasValue]]])
    val allActivities = projectActivities ++ taskActivities

    // Filter recent activities
    val recentActivities = allActivities.sortBy(-_.status().timestamp).take(maxCount)

    // Get all statuses
    val statuses = recentActivities.map(JsonSerializer.activityStatus)

    Ok(JsArray(statuses))
  }

  @deprecated
  def activityLog(): Action[AnyContent] = Action {
    Ok(JsonSerializer.logRecords(ActivityLog.records))
  }

  @Operation(
    summary = "Get activity status updates",
    description = "Request updates on the status of one or many activities.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json"
        ))
      )
    ))
  def getActivityStatusUpdates(@Parameter(
                                 name = "project",
                                 description = "Optional project identifier. If empty or not provided, activities from all projects are considered.",
                                 required = false,
                                 in = ParameterIn.QUERY,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               projectName: String,
                               @Parameter(
                                 name = "task",
                                 description = "Optional task identifier. If empty or not provided, activities from all tasks are considered.",
                                 required = false,
                                 in = ParameterIn.QUERY,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               taskName: String,
                               @Parameter(
                                 name = "activity",
                                 description = "Optional activity identifier. If empty or not provided, updates from all activities that match the task and project are returned.",
                                 required = false,
                                 in = ParameterIn.QUERY,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               activityName: String,
                               @Parameter(
                                 name = "instance",
                                 description = "Optional activity instance identifier. Non-singleton activity types allow multiple concurrent instances that are identified by their instance id.",
                                 required = false,
                                 in = ParameterIn.QUERY,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               activityInstance: String,
                               @Parameter(
                                 name = "timestamp",
                                 description = "Only return status updates that happened after this timestamp. Provided in milliseconds since midnight, January 1, 1970 UTC. If not provided or 0, the stati of all matching activities are returned.",
                                 required = false,
                                 in = ParameterIn.QUERY,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               timestamp: Long): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    implicit val writeContext = WriteContext.empty[JsValue]
    val activities = allActivities(projectName, taskName, activityName)
    val startTime = System.currentTimeMillis()

    Ok(
      Json.obj(
        "timestamp" -> startTime,
        "updates" ->
          JsArray(
            for(activity <- activities if activity.status().timestamp > timestamp) yield {
              val activityControl = activityControlForInstance(activity, activityInstance)
              new ExtendedStatusJsonFormat(activity).write(activityControl.status())
            }
          )
      )
    )
  }

  @Operation(
    summary = "Get activity value updates",
    description = "Request updates on the values of one or many activities.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json"
        ))
      )
    ))
  def getActivityValueUpdates(@Parameter(
                                 name = "project",
                                 description = "Optional project identifier. If empty or not provided, activities from all projects are considered.",
                                 required = false,
                                 in = ParameterIn.QUERY,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               projectName: String,
                               @Parameter(
                                 name = "task",
                                 description = "Optional task identifier. If empty or not provided, activities from all tasks are considered.",
                                 required = false,
                                 in = ParameterIn.QUERY,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               taskName: String,
                               @Parameter(
                                 name = "activity",
                                 description = "Optional activity identifier. If empty or not provided, updates from all activities that match the task and project are returned.",
                                 required = false,
                                 in = ParameterIn.QUERY,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               activityName: String,
                               @Parameter(
                                 name = "instance",
                                 description = "Optional activity instance identifier. Non-singleton activity types allow multiple concurrent instances that are identified by their instance id.",
                                 required = false,
                                 in = ParameterIn.QUERY,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               activityInstance: String,
                               @Parameter(
                                 name = "timestamp",
                                 description = "Only return status updates that happened after this timestamp. Provided in milliseconds since midnight, January 1, 1970 UTC. If not provided or 0, the stati of all matching activities are returned.",
                                 required = false,
                                 in = ParameterIn.QUERY,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               timestamp: Long): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    implicit val writeContext = WriteContext.empty[JsValue]
    val activities = allActivities(projectName, taskName, activityName)
    val startTime = System.currentTimeMillis()

    Ok(
      Json.obj(
        "timestamp" -> startTime,
        "updates" ->
          JsArray(
            for (activity <- activities if activity.status().timestamp > timestamp) yield {
              val activityControl = activityControlForInstance(activity, activityInstance)
              val value = activityControl.value()
              val format = Serialization.formatForDynamicType[JsValue](value.getClass)
              format.write(value)
            }
          )
      )
    )
  }

  @Operation(
    summary = "Get activity status updates (websocket)",
    description = "Request updates on the status of one or many activities.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json"
        ))
      )
    ))
  def activityStatusUpdatesWebSocket(@Parameter(
                                      name = "project",
                                      description = "Optional project identifier. If empty or not provided, activities from all projects are considered.",
                                      required = false,
                                      in = ParameterIn.QUERY,
                                      schema = new Schema(implementation = classOf[String])
                                    )
                                     projectName: String,
                                     @Parameter(
                                       name = "task",
                                       description = "Optional task identifier. If empty or not provided, activities from all tasks are considered.",
                                       required = false,
                                       in = ParameterIn.QUERY,
                                       schema = new Schema(implementation = classOf[String])
                                     )
                                     taskName: String,
                                     @Parameter(
                                       name = "activity",
                                       description = "Optional activity identifier. If empty or not provided, updates from all activities that match the task and project are returned.",
                                       required = false,
                                       in = ParameterIn.QUERY,
                                       schema = new Schema(implementation = classOf[String])
                                     )
                                     activityName: String,
                                     @Parameter(
                                       name = "instance",
                                       description = "Optional activity instance identifier. Non-singleton activity types allow multiple concurrent instances that are identified by their instance id.",
                                       required = false,
                                       in = ParameterIn.QUERY,
                                       schema = new Schema(implementation = classOf[String])
                                     )
                                     activityInstance: String): WebSocket = {

    implicit val userContext = UserContext.Empty
    implicit val writeContext = WriteContext.empty[JsValue]

    val activities = allActivities(projectName, taskName, activityName)
    val sources =
      for(activity <- activities) yield {
        implicit val format = new ExtendedStatusJsonFormat(activity)
        val activityControl = activityControlForInstance(activity, activityInstance)
        AkkaUtils.createSource(activityControl.status).map(format.write)
      }

    // Combine all sources into a single flow
    val combinedSources = Source.combine(Source.empty, Source.empty, sources :_*)(Merge(_))
    AkkaUtils.createWebSocket(combinedSources)
  }

  @Operation(
    summary = "Get activity value updates (websocket)",
    description = "Request updates on the values of one or many activities.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json"
        ))
      )
    ))
  def activityValuesUpdatesWebSocket(@Parameter(
                                       name = "project",
                                       description = "Optional project identifier. If empty or not provided, activities from all projects are considered.",
                                       required = false,
                                       in = ParameterIn.QUERY,
                                       schema = new Schema(implementation = classOf[String])
                                     )
                                     projectName: String,
                                     @Parameter(
                                       name = "task",
                                       description = "Optional task identifier. If empty or not provided, activities from all tasks are considered.",
                                       required = false,
                                       in = ParameterIn.QUERY,
                                       schema = new Schema(implementation = classOf[String])
                                     )
                                     taskName: String,
                                     @Parameter(
                                       name = "activity",
                                       description = "Optional activity identifier. If empty or not provided, updates from all activities that match the task and project are returned.",
                                       required = false,
                                       in = ParameterIn.QUERY,
                                       schema = new Schema(implementation = classOf[String])
                                     )
                                     activityName: String,
                                     @Parameter(
                                       name = "instance",
                                       description = "Optional activity instance identifier. Non-singleton activity types allow multiple concurrent instances that are identified by their instance id.",
                                       required = false,
                                       in = ParameterIn.QUERY,
                                       schema = new Schema(implementation = classOf[String])
                                     )
                                     activityInstance: String,
                                     @Parameter(
                                       name = "minIntervalMs",
                                       description = "Minimum number of milliseconds between updates.",
                                       required = false,
                                       in = ParameterIn.QUERY,
                                       schema = new Schema(implementation = classOf[String], defaultValue = "1000")
                                     )
                                     minIntervalMs: Int): WebSocket = {

    implicit val userContext = UserContext.Empty
    implicit val writeContext = WriteContext.empty[JsValue]

    val activities = allActivities(projectName, taskName, activityName)
    val sources =
      for (activity <- activities) yield {
        val format = Serialization.formatForDynamicType[JsValue](activity.value().getClass)
        val activityControl = activityControlForInstance(activity, activityInstance)
        AkkaUtils.createSource(activityControl.value, Some(FiniteDuration(minIntervalMs, MILLISECONDS))).map(format.write)
      }

    // Combine all sources into a single flow
    val combinedSources = Source.combine(Source.empty, Source.empty, sources: _*)(Merge(_))
    AkkaUtils.createWebSocket(combinedSources)
  }

  @Operation(
    summary = "Activity error report",
    description = "Get a detailed activity error report for a specific workspace, project or task activity.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(ActivityApiDoc.activityErrorReportJsonExample))
          ),
          new Content(
            mediaType = "text/markdown",
            examples = Array(new ExampleObject(ActivityApiDoc.activityErrorReportMarkdownExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the activity does not exist or there is no error report because the activity has not failed executing."
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If the parameter combination is invalid."
      )
    ))
  def activityErrorReport(@Parameter(
                            name = "project",
                            description = "Optional project identifier. If empty or not provided, activities from all projects are considered.",
                            in = ParameterIn.QUERY,
                            schema = new Schema(implementation = classOf[String])
                          )
                          projectId: String,
                          @Parameter(
                            name = "task",
                            description = "Optional task identifier. If empty or not provided, activities from all tasks are considered.",
                            in = ParameterIn.QUERY,
                            schema = new Schema(implementation = classOf[String])
                          )
                          taskId: String,
                          @Parameter(
                            name = "activity",
                            description = "Optional activity identifier. If empty or not provided, updates from all activities that match the task and project are returned.",
                            in = ParameterIn.QUERY,
                            required = true,
                            schema = new Schema(implementation = classOf[String])
                          )
                          activityId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val activity = fetchActivity(projectId, taskId, activityId)
    activity.status.get match {
      case Some(status) =>
        if(status.failed) {
          assert(status.exception.isDefined, "No exception available on failed activity status.")
          val (markdownHeader, errorReport) = activityErrorReport(projectId, taskId, activityId, status.exception.get)
          render {
            case AcceptsMarkdown() =>
              Ok(markdownHeader + errorReport.asMarkdown(None)).as(MARKDOWN_MIME)
            case Accepts.Json() => // default is JSON
              Ok(Json.toJson(errorReport))
          }
        } else {
          throw NotFoundException("Activity did not fail executing. No error report available.")
        }
      case None =>
        throw NotFoundException("Activity has not been run, yet. No error report available.")
    }
  }

  // Returns the markdown header and the error report.
  private def activityErrorReport(projectId: String, taskId: String, activityId: String, cause: Throwable)
                                 (implicit userContext: UserContext): (String, ErrorReportItem) = {
    def strToOption(str: String): Option[String] = if(str.trim != "") Some(str) else None
    val projectOpt = strToOption(projectId).map(getProject)
    val taskOpt = strToOption(taskId).map(id => anyTask(projectId, id))
    val errorReport = ErrorReportItem(
      projectId = projectOpt.map(_.id),
      projectLabel = projectOpt.filter(p => p.config.metaData.label.nonEmpty && p.id.toString != p.config.metaData.label.get).flatMap(_.config.metaData.label),
      taskId = taskOpt.map(_.id),
      taskLabel = taskOpt.map(t => t.metaData.formattedLabel(t.id, Int.MaxValue)),
      taskDescription = taskOpt.flatMap(_.metaData.description),
      activityId = Some(activityId),
      errorSummary = s"Execution of activity '$activityId' has failed.",
      errorMessage = Option(cause.getMessage),
      stackTrace = Some(Stacktrace.fromException(cause))
    )
    val projectPart = projectOpt.map(p => s" ${if(taskOpt.isDefined) "in" else "of"} project '${p.config.metaData.formattedLabel(p.id, Int.MaxValue)}'").getOrElse("")
    val taskPart = taskOpt.map(t => s" of task '${t.metaData.formattedLabel(t.id, Int.MaxValue)}'").getOrElse("")
    val markdownHeader = s"""# Activity execution error report
                            |
                            |Execution of activity '$activityId'$projectPart$taskPart has failed.
                            |
                            |""".stripMargin
    (markdownHeader, errorReport)
  }

  private def fetchActivity(projectId: String,
                            taskId: String,
                            activityId: String)
                           (implicit userContext: UserContext): WorkspaceActivity[_ <: HasValue] = {
    (projectId.trim, taskId.trim, activityId.trim) match {
      case (_, _, "") =>
        throw BadUserInputException("The activity parameter must be defined!")
      case ("", "", activityId) =>
        WorkspaceFactory().workspace.activity(activityId)
      case (projectId, "", activityId) =>
        getProject(projectId).activity(activityId)
      case ("", _, _) =>
        throw BadUserInputException("Project parameter must be given if task parameter is defined!")
      case (projectId, taskId, activityId) =>
        anyTask(projectId, taskId).activity(activityId)
    }
  }

  /**
    * Retrieves a single workspace activity.
    */
  private def activityControl(projectName: String, taskName: String, activityName: String, activityInstance: String)
                             (implicit userContext: UserContext): (ActivityControl[_], String) = {
    val activity = ActivityFacade.getActivity(projectName, taskName, activityName)
    (activityControlForInstance(activity, activityInstance), activity.label)
  }

  /**
    * Retrieves all workspace activities that satisfy the filter conditions.
    */
  private def allActivities(projectName: String, taskName: String, activityName: String)
                           (implicit userContext: UserContext): Seq[WorkspaceActivity[_]] = {
    val projects: Seq[Project] =
      if (projectName.nonEmpty) {
        Seq(WorkspaceFactory().workspace.project(projectName))
      } else {
        WorkspaceFactory().workspace.projects
      }

    def tasks(project: Project): Seq[ProjectTask[_ <: TaskSpec]] =
      if(taskName.nonEmpty) {
        Seq(project.anyTask(taskName))
      } else {
        project.allTasks
      }

    def activities(task: ProjectTask[_ <: TaskSpec]): Seq[WorkspaceActivity[_]] = {
      if (activityName.nonEmpty) {
        Seq(task.activity(activityName))
      } else {
        task.activities
      }
    }

    val workspaceActivities: Seq[WorkspaceActivity[_]] = {
      if(taskName.nonEmpty || projectName.nonEmpty) {
        Seq.empty
      } else {
        if(activityName.nonEmpty) {
          Seq(WorkspaceFactory().workspace.activity(activityName))
        } else {
          WorkspaceFactory().workspace.activities
        }
      }
    }

    val projectActivities: Seq[WorkspaceActivity[_]] = {
      if (taskName.nonEmpty) {
        Seq.empty
      } else {
        for (project <- projects;
             activity <- project.activities) yield activity
      }
    }

    val taskActivities: Seq[WorkspaceActivity[_]] = {
      if(activityName.nonEmpty && workspaceActivities.nonEmpty) {
        Seq.empty
      } else {
        for(project <- projects;
            task <- tasks(project);
            activity <- activities(task)) yield activity
      }
    }

    workspaceActivities ++ projectActivities ++ taskActivities
  }

  private def activityControlForInstance(activity: WorkspaceActivity[_], activityInstance: String): ActivityControl[_] = {
    if(activityInstance.nonEmpty) {
      activity.instance(activityInstance)
    } else {
      activity.control
    }
  }

  private def activityConfig(request: Request[AnyContent], includeQueryParameters: Boolean = true): Map[String, String] = {
    request.body match {
      case AnyContentAsFormUrlEncoded(values) =>
        values.view.mapValues(_.head).toMap
      case _ if includeQueryParameters =>
        val ignoredQueryParameters = Set("project", "task", "activity")
        request.queryString.view.filterKeys(!ignoredQueryParameters.contains(_)).mapValues(_.head).toMap
      case _ =>
        Map.empty
    }
  }
}

/**
  * Holds the activities log.
  */
object ActivityLog extends java.util.logging.Handler {

  private val size = 100

  private val buffer = Array.fill[LogRecord](size)(null)

  private var start = 0

  private var count = 0

  private val log = Logger.getLogger(getClass.getName)

  private val activitiesLogger = Logger.getLogger(Activity.loggingPath)

  init()

  def init(): Unit = {
    activitiesLogger.addHandler(this)
    log.fine("Logging of activities started.")
  }

  /**
    * Retrieves the recent log records
    */
  def records: Seq[LogRecord] = synchronized {
    log.fine(s"Retrieving $count activity logs")
    for (i <- 0 until count) yield {
      buffer((start + i) % buffer.length)
    }
  }

  /**
    * Adds a new log record.
    */
  override def publish(record: LogRecord): Unit = synchronized {
    log.fine(s"Adding activities log for '${record.getLoggerName}'")
    val ix = (start + count) % buffer.length
    buffer(ix) = record
    if (count < buffer.length) {
      count += 1
    }
    else {
      start += 1
      start %= buffer.length
    }
  }

  override def flush(): Unit = {}

  override def close(): Unit = {}
}
