package controllers.workspaceApi

import akka.actor.ActorSystem
import akka.stream.Materializer
import controllers.core.UserContextActions
import controllers.util.AkkaUtils
import controllers.workspaceApi.doc.ReportsApiDoc
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.ActivitySerializers.{ActivityExecutionResultJsonFormat, ExtendedStatusJsonFormat}
import org.silkframework.serialization.json.ExecutionReportSerializers.{ExecutionReportJsonFormat, WorkflowTaskReportJsonFormat}
import org.silkframework.util.Identifier
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow}
import org.silkframework.workspace.reports.{ExecutionReportManager, ReportIdentifier}
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{Action, AnyContent, InjectedController, WebSocket}

import java.time.Instant
import javax.inject.Inject

@Tag(name = "Execution reports", description = "List and retrieve workflow execution reports.")
class ReportsApi @Inject() (implicit system: ActorSystem, mat: Materializer) extends InjectedController with UserContextActions {

  @Operation(
    summary = "List reports",
    description = "Lists all available execution reports. Will only return reports if a execution report manager is configured.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(ReportsApiDoc.reportListExample))
        ))
      )
    ))
  def listReports(@Parameter(
                    name = "projectId",
                    description = "If provided, only return reports from the given project",
                    required = false,
                    in = ParameterIn.QUERY,
                    schema = new Schema(implementation = classOf[String])
                  )
                  projectId: Option[String],
                  @Parameter(
                    name = "taskId",
                    description = "If provided, only return reports from the given task",
                    required = false,
                    in = ParameterIn.QUERY,
                    schema = new Schema(implementation = classOf[String])
                  )
                  taskId: Option[String]): Action[AnyContent] = Action(parse.json) {
    val reports = ExecutionReportManager().listReports(projectId.map(Identifier(_)), taskId.map(Identifier(_)))
    val jsonObjects =
      for(report <- reports.sortBy(_.time)(Ordering[Instant].reverse)) yield {
        Json.obj(
          "project" -> report.projectId.toString,
          "task" -> report.taskId.toString,
          "time" -> report.time
        )
      }
    Ok(JsArray(jsonObjects))
  }

  @Operation(
    summary = "Retrieve report",
    description = "Retrieves an individual execution report. Typically, /list is called first in order to enumerate all available reports and their attributes.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(ReportsApiDoc.reportExample))
        ))
      )
    ))
  def retrieveReport(@Parameter(
                       name = "projectId",
                       description = "The project id of the report",
                       required = true,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[String])
                     )
                     projectId: String,
                     @Parameter(
                       name = "taskId",
                       description = "The task id of the report",
                       required = true,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[String])
                     )
                     taskId: String,
                     @Parameter(
                       name = "time",
                       description = "The timestamp of the report",
                       required = true,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[String])
                     )
                     time: String): Action[AnyContent] = Action(parse.json) {
    implicit val wc: WriteContext[JsValue] = new WriteContext[JsValue]()
    val report = ExecutionReportManager().retrieveReport(ReportIdentifier(projectId, taskId, Instant.parse(time)))
    val jsonFormat = new ActivityExecutionResultJsonFormat()(ExecutionReportJsonFormat)
    Ok(jsonFormat.write(report))
  }

  @Operation(
    summary = "Report updates",
    description = "Retrieves updates of the current execution report.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json"
        ))
      )
    ))
  def currentReportUpdates(@Parameter(
                             name = "projectId",
                             description = "The project id of the report",
                             required = true,
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[String])
                           )
                           projectId: String,
                           @Parameter(
                             name = "taskId",
                             description = "The task id of the report",
                             required = true,
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[String])
                           )
                           taskId: String,
                           @Parameter(
                             name = "timestamp",
                             description = "Only return status updates that happened after this timestamp. Provided in milliseconds since midnight, January 1, 1970 UTC. If not provided or 0, all updates will be returned.",
                             required = false,
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[String])
                           )
                           timestamp: Long): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    implicit val writeContext: WriteContext[JsValue] = WriteContext[JsValue]()
    val project = WorkspaceFactory().workspace.project(projectId)
    val workflow = project.task[Workflow](taskId)
    // TODO if we are running on Spark, this should use the respective activity
    val activity = workflow.activity[LocalWorkflowExecutorGeneratingProvenance]
    val startTime = System.currentTimeMillis()
    val report = activity.value().report

    Ok(
      Json.obj(
        "timestamp" -> startTime,
        "updates" ->
          JsArray(
            for(taskReport <- report.taskReports if taskReport.timestamp.toEpochMilli > timestamp) yield {
              WorkflowTaskReportJsonFormat.writeSummary(taskReport)
            }
          )
      )
    )
  }

  @Operation(
    summary = "Report updates (websocket)",
    description = "Retrieves updates of the current execution report.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json"
        ))
      )
    ))
  def currentReportUpdatesWebsocket(@Parameter(
                                      name = "projectId",
                                      description = "The project id of the report",
                                      required = true,
                                      in = ParameterIn.QUERY,
                                      schema = new Schema(implementation = classOf[String])
                                    )
                                    projectId: String,
                                    @Parameter(
                                      name = "taskId",
                                      description = "The task id of the report",
                                      required = true,
                                      in = ParameterIn.QUERY,
                                      schema = new Schema(implementation = classOf[String])
                                    )
                                    taskId: String): WebSocket = {
    implicit val userContext: UserContext = UserContext.Empty
    implicit val writeContext: WriteContext[JsValue] = WriteContext[JsValue]()

    val project = WorkspaceFactory().workspace.project(projectId)
    val workflow = project.task[Workflow](taskId)
    // TODO if we are running on Spark, this should use the respective activity
    val activity = workflow.activity[LocalWorkflowExecutorGeneratingProvenance]
    implicit val format = new ExtendedStatusJsonFormat(activity)

    val source = AkkaUtils.createSource(activity.status).map(format.write)
    AkkaUtils.createWebSocket(source)
  }

}