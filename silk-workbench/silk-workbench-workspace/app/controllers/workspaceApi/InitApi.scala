package controllers.workspaceApi

import java.util.logging.Logger

import com.typesafe.config.ConfigValueType
import controllers.core.RequestUserContextAction
import controllers.core.util.ControllerUtilsTrait
import javax.inject.Inject
import org.silkframework.config.DefaultConfig
import play.api.libs.json.{Format, JsArray, JsString, Json}
import play.api.mvc.{Action, AnyContent, InjectedController, Request}

import scala.collection.JavaConverters._

/**
  * API endpoints for initialization of the frontend application.
  */
case class InitApi @Inject()() extends InjectedController with ControllerUtilsTrait {
  private val dmConfigKey = "eccencaDataManager.baseUrl"
  private val dmLinksKey = "eccencaDataManager.moduleLinks"
  private val hotkeyConfigPath = "frontend.hotkeys"
  private val dmLinkPath = "path"
  private val dmLinkIcon = "icon"
  private val dmLinkDefaultLabel = "defaultLabel"
  private val playMaxFileUploadSizeKey = "play.http.parser.maxDiskBuffer"
  private lazy val cfg = DefaultConfig.instance()
  private val log: Logger = Logger.getLogger(getClass.getName)

  lazy val dmBaseUrl: Option[JsString] = {
    if(cfg.hasPath(dmConfigKey)) {
      Some(JsString(cfg.getString(dmConfigKey)))
    } else {
      None
    }
  }

  def init(): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val emptyWorkspace = workspace.projects.isEmpty
    val resultJson = Json.obj(
      "emptyWorkspace" -> emptyWorkspace,
      "initialLanguage" -> initialLanguage(request),
      "hotKeys" -> Json.toJson(hotkeys()),
      "maxFileUploadSize" -> maxUploadSize
    )
    val withDmUrl = dmBaseUrl.map { url =>
      resultJson + ("dmBaseUrl" -> url) + ("dmModuleLinks" -> JsArray(dmLinks.map(Json.toJson(_))))
    }.getOrElse(resultJson)
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

  private def maxUploadSize = {
    if (cfg.hasPath(playMaxFileUploadSizeKey)) {
      Some(cfg.getMemorySize(playMaxFileUploadSizeKey).toBytes)
    } else {
      None
    }
  }

  private def hotkeys(): Map[String, String] = {
    if(cfg.hasPath(hotkeyConfigPath)) {
      val hotkeyConfig = cfg.getConfig(hotkeyConfigPath)
      (for(entry <- hotkeyConfig.entrySet().asScala if entry.getValue.valueType() == ConfigValueType.STRING) yield {
        (entry.getKey, entry.getValue.unwrapped().toString)
      }).toMap
    } else {
      Map.empty
    }
  }

  /** Manually configured links into DM modules. */
  lazy val dmLinks: Seq[DmLink] = {
    if(cfg.hasPath(dmLinksKey)) {
      val linkConfig = cfg.getConfigList(dmLinksKey)
      var result: Vector[DmLink] = Vector.empty
      for(link <- linkConfig.asScala) {
        if(link.hasPath(dmLinkPath) && link.hasPath(dmLinkDefaultLabel)) {
          var icon: Option[String] = None
          if(link.hasPath(dmLinkIcon)) {
            icon = Some(link.getString(dmLinkIcon))
          }
          result :+= DmLink(link.getString(dmLinkPath).stripPrefix("/"), link.getString(dmLinkDefaultLabel), icon)
        } else {
          log.warning(s"Invalid entries in DM module links. Check '$dmLinksKey' in your config. Each link entry needs a '$dmLinkPath' and " +
              s"'$dmLinkDefaultLabel' value.")
        }
      }
      result
    } else {
      Seq.empty
    }
  }

  case class DmLink(path: String, defaultLabel: String, icon: Option[String])

  object DmLink {
    implicit val dmLinkFormat: Format[DmLink] = Json.format[DmLink]
  }
}
