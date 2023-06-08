package controllers.projectApi.requests

import org.silkframework.runtime.plugin.ParameterValues
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.PluginSerializers.ParameterValuesJsonFormat
import org.silkframework.serialization.json.{JsonFormat, JsonHelpers}
import play.api.libs.json.{JsValue, Json}

case class ReloadFailedTaskRequest(taskId: String,
                                   parameterValues: Option[ParameterValues])

object ReloadFailedTaskRequest {
  implicit val reloadFailedTaskRequestFormat: JsonFormat[ReloadFailedTaskRequest] = new JsonFormat[ReloadFailedTaskRequest] {
    final val TASK_ID = "taskId"
    final val PARAMETER_VALUES = "parameterValues"

    override def read(value: JsValue)(implicit readContext: ReadContext): ReloadFailedTaskRequest = {
      val taskId = JsonHelpers.stringValue(value, TASK_ID)
      val parameterValues = JsonHelpers.optionalValue(value, PARAMETER_VALUES).map(js => ParameterValuesJsonFormat.read(js))
      ReloadFailedTaskRequest(taskId, parameterValues)
    }

    override def write(value: ReloadFailedTaskRequest)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        TASK_ID -> value.taskId,
        PARAMETER_VALUES -> ParameterValuesJsonFormat.write(value.parameterValues.getOrElse(ParameterValues.empty))
      )
    }
  }
}