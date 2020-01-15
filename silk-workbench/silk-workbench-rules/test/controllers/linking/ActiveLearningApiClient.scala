package controllers.linking

import helper.ApiClient
import org.silkframework.entity.Link
import controllers.linking.routes.ActiveLearningApi
import org.silkframework.rule.LinkageRule
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.serialization.json.JsonSerializers.LinkageRuleJsonFormat
import org.silkframework.serialization.json.LinkingSerializers.LinkJsonFormat
import play.api.http.Status
import play.api.libs.json.JsValue
import play.api.libs.ws.EmptyBody

trait ActiveLearningApiClient extends ApiClient {

  def iterate(projectId: String, taskId: String, decision: String, linkSource: String, linkTarget: String): Option[Link] = {
    val request = createRequest(ActiveLearningApi.iterate(projectId, taskId, decision, linkSource, linkTarget))
    val response = checkResponse(request.post(EmptyBody))

    if(response.status == Status.NO_CONTENT) {
      None
    } else {
      implicit val redContext = ReadContext()
      Some(LinkJsonFormat.read(response.body[JsValue]))
    }
  }

  def bestRule(projectId: String, taskId: String): LinkageRule = {
    val request = createRequest(ActiveLearningApi.bestRule(projectId, taskId))
    val response = checkResponse(request.get())
    implicit val redContext = ReadContext()
    LinkageRuleJsonFormat.read(response.json)
  }
}
