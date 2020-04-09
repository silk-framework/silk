package controllers.workspaceApi

import helper.IntegrationTestTrait
import org.scalatest.{FlatSpec, MustMatchers}
import play.api.libs.json.{JsBoolean, JsObject, JsValue}
import play.api.routing.Router

class InitApiTest extends FlatSpec with IntegrationTestTrait with MustMatchers {
  behavior of "Init API"

  override def workspaceProviderId: String = "inMemory"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[test.Routes])

  it should "show if the workspace is (not) empty" in {
    initFrontendResult().get("emptyWorkspace") mustBe Some(JsBoolean(true))
    createProject("someProject")
    initFrontendResult().get("emptyWorkspace") mustBe Some(JsBoolean(false))
  }

  private def initFrontendResult(): collection.Map[String, JsValue] = {
    checkResponse(client.url(s"$baseUrl/api/workspace/initFrontend").get()).json.as[JsObject].value
  }
}
