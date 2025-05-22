package controllers.workspaceApi.search

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.runtime.plugin.{ParameterStringValue, ParameterTemplateValue, SimpleParameterValue}
import play.api.libs.json._

import scala.language.implicitConversions

/**
  * A request for auto-completion results for a plugin parameter.
  *
  * @param pluginId                 The ID of the plugin.
  * @param parameterId              The ID of the plugin parameter.
  * @param projectId                The project ID that will be passed to the auto-completion service provider.
  * @param dependsOnParameterValues The parameter values this parameter auto-completion depends on. These dependencies are
  *                                 stated in the @Param annotation of the parameter.
  * @param textQuery                The text query to filter the results.
  * @param offset                   Offset for paging.
  * @param limit                    Limit for paging.
  */
case class ParameterAutoCompletionRequest(pluginId: String,
                                          parameterId: String,
                                          projectId: String,
                                          @ArraySchema(
                                            schema = new Schema(
                                              description = "The parameter values this parameter auto-completion depends on. These dependencies are stated in the @Param annotation of the parameter.",
                                              implementation = classOf[String]
                                            ))
                                          dependsOnParameterValues: Option[Seq[ParameterValueJson]] = None,
                                          textQuery: Option[String] = None,
                                          offset: Option[Int] = None,
                                          limit: Option[Int] = None) {
  def workingLimit: Int = limit.filter(_ > 0).getOrElse(ParameterAutoCompletionRequest.DEFAULT_LIMIT)
  def workingOffset: Int = offset.filter(_ > 0).getOrElse(ParameterAutoCompletionRequest.DEFAULT_OFFSET)
}

object ParameterAutoCompletionRequest {
  final val DEFAULT_OFFSET = 0
  final val DEFAULT_LIMIT = 10
  implicit val parameterAutoCompletionRequestJsonFormat: Format[ParameterAutoCompletionRequest] = Json.format[ParameterAutoCompletionRequest]
}

@Schema(description = "The parameter value for auto-completion.")
case class ParameterValueJson(@Schema(description = "The parameter value.")
                              value: String,
                              @Schema(description = "Signals if the parameter value is a template that needs to be resolved.")
                              isTemplate: Boolean) {
  def toParameterValue: SimpleParameterValue = {
    if(isTemplate) {
      ParameterTemplateValue(value)
    } else {
      ParameterStringValue(value)
    }
  }
}

object ParameterValueJson {

  implicit def fromString(value: String): ParameterValueJson = {
    ParameterValueJson(value, isTemplate = false)
  }

  /**
   * Reads and writes a JSON value into/from a ParameterValueJson object.
   * Also supports legacy clients that just send a string instead of a JSON object.
   */
  implicit val parameterValueJsonFormat: Format[ParameterValueJson] = new Format[ParameterValueJson] {

    private val objectFormat = Json.format[ParameterValueJson]

    override def reads(json: JsValue): JsResult[ParameterValueJson] = json match {
      case JsString(s) =>
        JsSuccess(ParameterValueJson(s, isTemplate = false))
      case obj: JsObject =>
        objectFormat.reads(obj)
      case _ =>
        JsError("Expected string or object")
    }

    override def writes(o: ParameterValueJson): JsValue = {
      objectFormat.writes(o)
    }
  }
}
