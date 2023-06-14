package controllers.projectApi.requests

import io.swagger.v3.oas.annotations.media.Schema
import org.silkframework.runtime.plugin.ParameterValues
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.PluginSerializers.ParameterValuesJsonFormat
import org.silkframework.serialization.json.{JsonFormat, JsonHelpers}
import org.silkframework.workspace.OriginalTaskData
import play.api.libs.json.{Format, JsValue, Json}

@Schema(
  description = "Request object to reload a failed task.",
  implementation = classOf[Object],
)
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

object OriginalTaskDataResponse {
  final val PLUGIN_ID = "pluginId"
  final val PARAMETER_VALUES = "parameterValues"

  val OriginalTaskDataJsonFormat: JsonFormat[OriginalTaskData] = new JsonFormat[OriginalTaskData] {
    override def read(value: JsValue)(implicit readContext: ReadContext): OriginalTaskData = {
      val pluginId = JsonHelpers.stringValue(value, PLUGIN_ID)
      val parameterValues = ParameterValuesJsonFormat.read(JsonHelpers.requiredValue(value, PARAMETER_VALUES))
      OriginalTaskData(pluginId, parameterValues)
    }

    override def write(value: OriginalTaskData)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        PLUGIN_ID -> value.pluginId,
        PARAMETER_VALUES -> ParameterValuesJsonFormat.write(value.parameterValues)
      )
    }
  }
}