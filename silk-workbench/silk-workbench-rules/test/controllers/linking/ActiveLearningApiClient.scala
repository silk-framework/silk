package controllers.linking

import controllers.linking.activeLearning.JsonFormats.ComparisonPairFormat
import helper.ApiClient
import org.silkframework.entity.Link
import controllers.linking.routes.ActiveLearningApi
import org.silkframework.rule.LinkageRule
import controllers.linking.activeLearning.JsonFormats._
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.serialization.json.JsonSerializers.LinkageRuleJsonFormat
import org.silkframework.serialization.json.LinkingSerializers.LinkJsonFormat
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.EmptyBody

trait ActiveLearningApiClient extends ApiClient {

  def addComparisonPair(projectId: String, taskId: String, comparisonPair: ComparisonPairFormat): Unit = {
    val request = createRequest(ActiveLearningApi.addComparisonPair(projectId, taskId))
    checkResponse(request.post(Json.toJson(comparisonPair)))
  }

  def referenceLinks(projectId: String, taskId: String): JsValue = {
    val request = createRequest(ActiveLearningApi.referenceLinks(projectId, taskId))
    checkResponse(request.get()).json
  }

  def iterate(projectId: String, taskId: String, decision: String, linkSource: String, linkTarget: String, synchronous: Boolean = false): Option[Link] = {
    val request = createRequest(ActiveLearningApi.iterate(projectId, taskId, decision, linkSource, linkTarget, synchronous))
    val response = checkResponse(request.post(EmptyBody))

    if(response.status == Status.NO_CONTENT) {
      None
    } else {
      implicit val readContext: ReadContext = ReadContext()
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
