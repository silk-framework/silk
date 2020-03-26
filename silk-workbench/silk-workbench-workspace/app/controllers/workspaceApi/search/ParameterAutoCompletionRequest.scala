package controllers.workspaceApi.search

import play.api.libs.json.{Format, Json}

/**
  * A request for auto-completion results for a plugin parameter.
  *
  * @param pluginId    The ID of the plugin.
  * @param parameterId The ID of the plugin parameter.
  * @param projectId   The project ID that will be passed to the auto-completion service provider.
  * @param textQuery   The text query to filter the results.
  * @param offset      Offset for paging.
  * @param limit       Limit for paging.
  */
case class ParameterAutoCompletionRequest(pluginId: String,
                                          parameterId: String,
                                          projectId: String,
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
