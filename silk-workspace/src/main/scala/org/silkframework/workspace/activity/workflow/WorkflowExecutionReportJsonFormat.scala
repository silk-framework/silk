package org.silkframework.workspace.activity.workflow

import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonFormat
import org.silkframework.util.Identifier
import play.api.libs.json._

case class WorkflowExecutionReportJsonFormat() extends JsonFormat[WorkflowExecutionReport] {

  /**
    * Deserializes a value.
    */
  override def read(value: JsValue)(implicit readContext: ReadContext): WorkflowExecutionReport = {
    ???
  }

  /**
    * Serializes a value.
    */
  override def write(value: WorkflowExecutionReport)(implicit writeContext: WriteContext[JsValue]): JsValue = {
    Json.obj(
      "taskReports" -> JsArray(value.taskReports.toSeq.map(serializeTaskReport))
    )
  }

  private def serializeTaskReport(taskAndreport: (Identifier, ExecutionReport)) = {
    val (task, report) = taskAndreport
    Json.obj(
      "task" -> task.toString,
      "summary" -> JsObject(for((key, value) <- report.summary) yield (key, JsString(value)))
    )
  }
}
