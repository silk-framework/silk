package controllers.workspaceApi

import akka.actor.ActorSystem
import akka.stream.Materializer
import controllers.core.UserContextActions
import controllers.util.AkkaUtils
import controllers.workspaceApi.doc.ReportsApiDoc
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.runtime.activity.{Observable, UserContext}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.ActivitySerializers.ActivityExecutionResultJsonFormat
import org.silkframework.serialization.json.ExecutionReportSerializers.ExecutionReportJsonFormat
import org.silkframework.util.Identifier
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowExecutionReportWithProvenance, WorkflowTaskReport}
import org.silkframework.workspace.reports.{ExecutionReportManager, ReportIdentifier}
import play.api.libs.json.{JsArray, JsValue, Json, OFormat}
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
    summary = "Current report",
    description = "Retrieves the current execution report.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json"
        ))
      )
    ))
  def currentReport(@Parameter(
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
                    taskId: String): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    implicit val writeContext: WriteContext[JsValue] = WriteContext[JsValue]()
    val report = retrieveCurrentReport(projectId, taskId).apply().report
    Ok(ExecutionReportJsonFormat.write(report))
  }

  @Operation(
    summary = "Report updates",
    description = "Retrieves updates of the current execution report.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(
            implementation = classOf[ReportUpdates]
          )
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
    val startTime = System.currentTimeMillis()
    val report = retrieveCurrentReport(projectId, taskId).apply().report

    val updates =
      for(taskReport <- report.taskReports if taskReport.timestamp.toEpochMilli > timestamp) yield {
        ReportSummary(taskReport)
      }

    Ok(Json.toJson(ReportUpdates(startTime, updates)))
  }

  @Operation(
    summary = "Report updates (websocket)",
    description = "Retrieves updates of the current execution report.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(
            implementation = classOf[ReportUpdates]
          )
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
    val currentReport = retrieveCurrentReport(projectId, taskId)

    var lastUpdate = Instant.EPOCH
    val source = AkkaUtils.createSource(currentReport).map { value =>
      val updates =
        for(taskReport <- value.report.taskReports if taskReport.timestamp isAfter lastUpdate) yield {
          ReportSummary(taskReport)
        }
      if(value.report.taskReports.nonEmpty) {
        lastUpdate = value.report.taskReports.maxBy(_.timestamp).timestamp
      }
      Json.toJson(ReportUpdates(lastUpdate.toEpochMilli, updates))
    }
    AkkaUtils.createWebSocket(source)
  }

  private def retrieveCurrentReport(projectId: String, taskId: String)(implicit userContext: UserContext): Observable[WorkflowExecutionReportWithProvenance] = {
    val project = WorkspaceFactory().workspace.project(projectId)
    val workflow = project.task[Workflow](taskId)
    val activity = workflow.activity("ExecuteDefaultWorkflow")
    activity.value.asInstanceOf[Observable[WorkflowExecutionReportWithProvenance]]
  }

}

@Schema(
  description = "Represents a set of updates to the execution status of individual workflow nodes."
)
case class ReportUpdates(@Schema(
                           description = "Timestamp (milliseconds since the epoch of 1970-01-01T00:00:00).",
                           required = true
                         )
                         timestamp: Long,
                         @ArraySchema(
                           schema = new Schema(
                             description = "All updates since the given timestamp. Contains a separate entry for each node that changed.",
                             implementation = classOf[ReportSummary]
                           )
                         )
                         updates: IndexedSeq[ReportSummary])

object ReportUpdates {
  implicit val reportUpdatesFormat: OFormat[ReportUpdates] = Json.format[ReportUpdates]
}

case class ReportSummary(@Schema(
                           description = "The identifier of the updated node.",
                           required = true
                         )
                         node: String,
                         @Schema(
                           description = "Timestamp of the last update to this node (milliseconds since the epoch of 1970-01-01T00:00:00).",
                           required = true
                         )
                         timestamp: Long,
                         @Schema(
                           description = "Short label for the executed operation, e.g., 'read' or 'write' (optional).",
                           required = false
                         )
                         operation: Option[String],
                         @ArraySchema(
                           schema = new Schema(
                             description = "If issues occurred during execution of this node, this contains a list of user-friendly messages.",
                             implementation = classOf[String]
                           )
                         )
                         warnings: Seq[String],
                         @Schema(
                           description = "Error message in case a fatal error occurred.",
                           required = false
                         )
                         error: Option[String],
                         @Schema(
                           description = "True, if the execution of this node finished. False, if the execution is still running.",
                         )
                         isDone: Boolean,
                         @Schema(
                           description = "The number of entities that have been processed.",
                         )
                         entityCount: Int)

object ReportSummary {

  implicit val reportSummaryFormat: OFormat[ReportSummary] = Json.format[ReportSummary]

  def apply(value: WorkflowTaskReport): ReportSummary = {
    val report = value.report
    ReportSummary(
      node = value.nodeId.toString,
      timestamp = value.timestamp.toEpochMilli,
      operation = report.operation,
      warnings = report.warnings,
      error = report.error,
      isDone = report.isDone,
      entityCount =report.entityCount
    )
  }
}