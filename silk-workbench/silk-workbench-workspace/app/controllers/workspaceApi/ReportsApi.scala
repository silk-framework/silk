package controllers.workspaceApi

import java.time.Instant

import javax.inject.Inject
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.ExecutionReportSerializers.ExecutionReportJsonFormat
import org.silkframework.util.Identifier
import org.silkframework.workspace.reports.ReportManager
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}

class ReportsApi @Inject() () extends InjectedController {

  def listReports(projectId: Option[String], taskId: Option[String]): Action[AnyContent] = Action(parse.json) {
    val jsonObjects =
      for(report <- ReportManager().listReports(projectId.map(Identifier(_)), taskId.map(Identifier(_)))) yield {
        Json.obj(
          "project" -> report.projectId.toString,
          "task" -> report.taskId.toString,
          "time" -> report.time
        )
      }
    Ok(JsArray(jsonObjects))
  }

  def retrieveReport(projectId: String, taskId: String, time: String): Action[AnyContent] = Action(parse.json) {
    implicit val wc: WriteContext[JsValue] = new WriteContext[JsValue]()
    val report = ReportManager().retrieveReport(projectId, taskId, Instant.parse(time))
    Ok(ExecutionReportJsonFormat.write(report))
  }

}
