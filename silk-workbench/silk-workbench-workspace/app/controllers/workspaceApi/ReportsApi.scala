package controllers.workspaceApi

import controllers.workspaceApi.doc.ReportsApiDoc
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.ActivitySerializers.ActivityExecutionResultJsonFormat
import org.silkframework.serialization.json.ExecutionReportSerializers.ExecutionReportJsonFormat
import org.silkframework.util.Identifier
import org.silkframework.workspace.reports.{ExecutionReportManager, ReportIdentifier}
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}

import java.time.Instant
import javax.inject.Inject

@Tag(name = "Execution reports", description = "List and retrieve workflow execution reports.")
class ReportsApi @Inject() () extends InjectedController {

  @Operation(
    summary = "List reports",
    description = "Lists all available execution reports. Will only return reports if a execution report manager is configured.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
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

}