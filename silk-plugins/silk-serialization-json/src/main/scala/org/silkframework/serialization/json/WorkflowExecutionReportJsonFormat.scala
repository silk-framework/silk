package org.silkframework.serialization.json

import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.WorkflowExecutionReport
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
