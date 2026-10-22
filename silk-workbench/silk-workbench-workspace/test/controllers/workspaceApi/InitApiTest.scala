package controllers.workspaceApi

import com.typesafe.config.ConfigFactory
import controllers.workspaceApi.InitApiTestUserManager._
import helper.IntegrationTestTrait
import org.silkframework.runtime.activity.{SimpleUserContext, UserContext}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.users.{UserActions, WebUser, WebUserManager}
import org.silkframework.serialization.json.JsonHelpers
import org.silkframework.util.ConfigTestTrait
import play.api.libs.json.{JsBoolean, JsObject, JsString, JsValue, Json}
import play.api.mvc.RequestHeader
import play.api.routing.Router
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class InitApiTest extends AnyFlatSpec with IntegrationTestTrait with Matchers with ConfigTestTrait {
  behavior of "Init API"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

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

  it should "enable the companion only for users with the companion action" in {
    companionConfig(companionUser) mustBe Json.obj(
      "enabled" -> true,
      "apiBasePath" -> "/dataplatform/api/companion",
      "streamPath" -> "/dataplatform/companion-websocket"
    )
    companionConfig(userWithoutCompanionAction) mustBe Json.obj(
      "enabled" -> false,
      "apiBasePath" -> "/dataplatform/api/companion",
      "streamPath" -> "/dataplatform/companion-websocket"
    )
  }

  it should "enable the companion for users with all actions" in {
    (companionConfig(adminUser) \ "enabled").as[Boolean] mustBe true
  }

  it should "default to a disabled companion when configuration is missing" in {
    CompanionFrontendConfig.fromConfig(ConfigFactory.empty(), UserActions.all) mustBe Right(
      CompanionFrontendConfig.disabled
    )
  }

  it should "keep the companion disabled when installation configuration disables it" in {
    val config = ConfigFactory.parseString(
      """
        |com.eccenca.di.assistant.CompanionConfig.enabled = false
        |""".stripMargin
    )
    CompanionFrontendConfig.fromConfig(config, UserActions.all).map(_.enabled) mustBe Right(false)
  }

  it should "reject companion URLs that are not same-origin absolute paths" in {
    val config = ConfigFactory.parseString(
      """
        |com.eccenca.di.assistant.CompanionConfig {
        |  enabled = true
        |  apiBasePath = "https://explore.example/api/companion"
        |  streamPath = "//explore.example/companion-websocket"
        |}
        |""".stripMargin
    )
    CompanionFrontendConfig.fromConfig(config, UserActions.all).isLeft mustBe true
  }

  private def initFrontendResult(httpHeaders: (String, String)*): collection.Map[String, JsValue] = {
    val request = client.url(s"$baseUrl/api/workspace/initFrontend").addHttpHeaders(httpHeaders :_*)
    checkResponse(request.get()).json.as[JsObject].value
  }

  private def companionConfig(user: WebUser): JsObject = {
    initFrontendResult("X-Forwarded-User" -> user.uri)("companion").as[JsObject]
  }

  override def propertyMap: Map[String, Option[String]] = {
    PluginRegistry.registerPlugin(classOf[InitApiTestUserManager])
    Map(
      "eccencaDataManager.baseUrl" -> Some(exampleUrl),
      "frontend.hotkeys.quickSearch" -> Some("/"),
      "com.eccenca.di.assistant.CompanionConfig.enabled" -> Some("true"),
      "com.eccenca.di.assistant.CompanionConfig.apiBasePath" -> Some("/dataplatform/api/companion"),
      "com.eccenca.di.assistant.CompanionConfig.streamPath" -> Some("/dataplatform/companion-websocket"),
      "user.manager.web.plugin" -> Some("initApiTestUserManager")
    )
  }
}

@Plugin(
  id = "initApiTestUserManager",
  label = "Init API test user manager",
  description = "Provides users with different companion actions for Init API tests."
)
class InitApiTestUserManager extends WebUserManager {
  override def user(request: RequestHeader): Option[WebUser] = {
    request.headers.get("X-Forwarded-User").flatMap(InitApiTestUserManager.usersByUri.get)
  }

  override def userContext(request: RequestHeader): UserContext = SimpleUserContext(user(request))
}

object InitApiTestUserManager {
  val companionAction = "https://vocab.eccenca.com/auth/Action/Explore-Companion-Use"
  val companionUser = new WebUser(
    "https://example.org/users/companion",
    Some("Companion user"),
    actions = UserActions(Set(companionAction))
  )
  val userWithoutCompanionAction = new WebUser(
    "https://example.org/users/without-companion",
    Some("User without companion"),
    actions = UserActions.empty
  )
  val adminUser = new WebUser(
    "https://example.org/users/admin",
    Some("Admin user"),
    actions = UserActions.all
  )
  val usersByUri: Map[String, WebUser] = Seq(companionUser, userWithoutCompanionAction, adminUser)
    .map(user => user.uri -> user)
    .toMap
}
