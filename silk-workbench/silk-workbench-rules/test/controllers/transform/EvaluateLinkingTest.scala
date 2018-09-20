package controllers.transform

import helper.IntegrationTestTrait
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.libs.json._
import play.api.libs.ws.WS

class EvaluateLinkingTest extends FlatSpec with IntegrationTestTrait with SingleProjectWorkspaceProviderTestTrait with MustMatchers {

  behavior of "EvaluateLinking"

  override def projectPathInClasspath: String = "controllers/transform/linkingMovies.zip"

  override def projectId: String = "movies"

  def linkingTaskId: String = "movies"

  override def routes: Option[String] = Some("test.Routes")

  override def workspaceProvider: String = "inMemory"

  // TODO this tests run standalone, but let other tests fail. Disabling for now
  ignore should "generate evaluation links" in {
    evaluateLinkingTask(projectId, linkingTaskId)
  }

  ignore should "return the generated links as JSON" in {
    val response = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$linkingTaskId/activities/EvaluateLinking/value")
                     .withHeaders("Accept" -> "application/json")
                     .get()
    val linkingResult = checkResponse(response).json

    val links = (linkingResult \ "links").as[JsArray].value
    links.size mustBe 110

    val sourceUri = "http://dbpedia.org/resource/Taxi_%281998_film%29"
    val targetUri = "http://data.linkedmdb.org/resource/film/863"
    val link = links.find(link => (link \ "source").get == JsString(sourceUri) && (link \ "target").get == JsString(targetUri))
    link.isDefined mustBe true
  }
}
