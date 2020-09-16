package controllers.workspaceApi

import helper.IntegrationTestTrait
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.util.ConfigTestTrait
import play.api.libs.json.{JsBoolean, JsObject, JsString, JsValue}
import play.api.routing.Router

class InitApiTest extends FlatSpec with IntegrationTestTrait with MustMatchers with ConfigTestTrait {
  behavior of "Init API"

  override def workspaceProviderId: String = "inMemory"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[test.Routes])

  it should "show if the workspace is (not) empty" in {
    initFrontendResult().get("emptyWorkspace") mustBe Some(JsBoolean(true))
    createProject("someProject")
    initFrontendResult().get("emptyWorkspace") mustBe Some(JsBoolean(false))
  }

  it should "return the initial language" in {
    initFrontendResult(("accept-language" -> "fr-CH, fr;q=0.9, de;q=0.8, en;q=0.7 *;q=0.5")).get("initialLanguage") mustBe Some(JsString("de"))
    initFrontendResult(("accept-language" -> "fr-CH, fr;q=0.9, *;q=0.5")).get("initialLanguage") mustBe Some(JsString("en"))
  }

  val exampleUrl = "http://example"
  it should "return the DM base URL if configured" in {
    initFrontendResult().get("dmBaseUrl") mustBe Some(JsString(exampleUrl))
  }

  private def initFrontendResult(httpHeaders: (String, String)*): collection.Map[String, JsValue] = {
    val request = client.url(s"$baseUrl/api/workspace/initFrontend").withHttpHeaders(httpHeaders :_*)
    checkResponse(request.get()).json.as[JsObject].value
  }

  override def propertyMap: Map[String, Option[String]] = Map(
    "eccencaDataManager.baseUrl" -> Some(exampleUrl)
  )
}
