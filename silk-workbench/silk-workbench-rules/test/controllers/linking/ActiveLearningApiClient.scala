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
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.EmptyBody

trait ActiveLearningApiClient extends ApiClient {

  def addComparisonPair(projectId: String, taskId: String, comparisonPair: ComparisonPairFormat): Unit = {
    val request = createRequest(ActiveLearningApi.addComparisonPair(projectId, taskId))
    checkResponse(request.post(Json.toJson(comparisonPair)))
  }

  def retrieveReferenceLinks(projectId: String, taskId: String): (Seq[Link], Seq[Link]) = {
    val request = createRequest(ActiveLearningApi.referenceLinks(projectId, taskId))
    val json = checkResponse(request.get()).json

    implicit val readContext: ReadContext = ReadContext()
    val positiveLinks = (json \ "positive").as[JsArray].value.map(new LinkJsonFormat(None).read)
    val negativeLinks = (json \ "negative").as[JsArray].value.map(new LinkJsonFormat(None).read)
    (positiveLinks, negativeLinks)
  }

  def addReferenceLink(projectId: String, taskId: String, linkSource: String, linkTarget: String, decision: String, synchronous: Boolean = false): Unit = {
    val request = createRequest(ActiveLearningApi.addReferenceLink(projectId, taskId, linkSource, linkTarget, decision, synchronous))
    checkResponse(request.post(EmptyBody))
  }

  def nextLinkCandidate(projectId: String, taskId: String): Option[Link] = {
    val request = createRequest(ActiveLearningApi.nextLinkCandidate(projectId, taskId))
    val response = checkResponse(request.get())

    if (response.status == Status.NO_CONTENT) {
      None
    } else {
      implicit val readContext: ReadContext = ReadContext()
      Some(LinkJsonFormat.read(response.body[JsValue]))
    }
  }

  def bestRule(projectId: String, taskId: String): LinkageRule = {
    val request = createRequest(ActiveLearningApi.bestRule(projectId, taskId))
    val response = checkResponse(request.get())
    implicit val readContext: ReadContext = ReadContext()
    LinkageRuleJsonFormat.read(response.json)
  }
}
