package controllers.workspaceApi

import helper.IntegrationTestTrait

import org.silkframework.serialization.json.JsonHelpers
import org.silkframework.util.ConfigTestTrait
import play.api.libs.json.{JsBoolean, JsObject, JsString, JsValue, Json}
import play.api.routing.Router
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class InitApiTest extends AnyFlatSpec with IntegrationTestTrait with Matchers with ConfigTestTrait {
  behavior of "Init API"

  override def workspaceProviderId: String = "inMemory"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkspace.Routes])

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

  it should "return configured hotkey values" in {
    initFrontendResult().get("hotKeys").map(jsValue => JsonHelpers.fromJsonValidated[Map[String, String]](jsValue)) mustBe Some(Map("quickSearch" -> "/"))
  }

  private def initFrontendResult(httpHeaders: (String, String)*): collection.Map[String, JsValue] = {
    val request = client.url(s"$baseUrl/api/workspace/initFrontend").addHttpHeaders(httpHeaders :_*)
    checkResponse(request.get()).json.as[JsObject].value
  }

  override def propertyMap: Map[String, Option[String]] = Map(
    "eccencaDataManager.baseUrl" -> Some(exampleUrl),
    "frontend.hotkeys.quickSearch" -> Some("/")
  )
}
