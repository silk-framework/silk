package controllers.transform

import helper.IntegrationTestTrait
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.libs.json._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class EvaluateLinkingTest extends AnyFlatSpec with IntegrationTestTrait with SingleProjectWorkspaceProviderTestTrait with Matchers {

  behavior of "EvaluateLinking"

  override def projectPathInClasspath: String = "controllers/transform/linkingMovies.zip"

  def linkingTaskId: String = "movies"

  override def routes = Some(classOf[test.Routes])

  override def workspaceProviderId: String = "inMemory"

  // TODO this tests run standalone, but let other tests fail. Disabling for now
  ignore should "generate evaluation links" in {
    evaluateLinkingTask(projectId, linkingTaskId)
  }

  ignore should "return the generated links as JSON" in {
    val response = client.url(s"$baseUrl/workspace/projects/$projectId/tasks/$linkingTaskId/activities/EvaluateLinking/value")
                     .addHttpHeaders("Accept" -> "application/json")
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
