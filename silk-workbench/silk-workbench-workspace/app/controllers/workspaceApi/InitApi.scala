package controllers.workspaceApi

import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.core.util.ControllerUtilsTrait
import javax.inject.Inject
import org.silkframework.config.DefaultConfig
import play.api.libs.json.{JsNull, JsString, Json}
import play.api.mvc.{Action, AnyContent, InjectedController, Request}

/**
  * API endpoints for initialization of the frontend application.
  */
case class InitApi @Inject()() extends InjectedController with ControllerUtilsTrait {
  private val dmConfigKey = "eccencaDataManager.baseUrl"
  private lazy val cfg = DefaultConfig.instance()

  def init(): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val emptyWorkspace = workspace.projects.isEmpty
    val resultJson = Json.obj(
      "emptyWorkspace" -> emptyWorkspace,
      "initialLanguage" -> initialLanguage(request)
    )
    val withDmUrl = dmBaseUrl.map(url => resultJson + ("dmBaseUrl" -> url)).getOrElse(resultJson)
    Ok(withDmUrl)
  }

  val supportedLanguages = Set("en", "de")

  /** The initial UI language, extracted from the accept-language header. */
  private def initialLanguage(request: Request[AnyContent]): String = {
    request.acceptLanguages.foreach(lang => {
      val countryCode = lang.code.take(2).toLowerCase
      if(supportedLanguages.contains(countryCode)) {
        return countryCode
      }
    })
    "en" // default
  }

  private def dmBaseUrl: Option[JsString] = {
    if(cfg.hasPath(dmConfigKey)) {
      Some(JsString(cfg.getString(dmConfigKey)))
    } else {
      None
    }
  }
}
