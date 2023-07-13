package controllers.workspaceApi

import java.util.logging.Logger
import com.typesafe.config.ConfigValueType
import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspaceApi.doc.InitApiDoc
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

import javax.inject.Inject
import org.silkframework.config.DefaultConfig
import org.silkframework.runtime.templating.GlobalTemplateVariablesConfig
import play.api.libs.json.{Format, JsArray, JsString, Json}
import play.api.mvc.{Action, AnyContent, InjectedController, Request}

import scala.jdk.CollectionConverters.CollectionHasAsScala

/**
  * API endpoints for initialization of the frontend application.
  */
@Tag(name = "Workbench")
case class InitApi @Inject()() extends InjectedController with UserContextActions with ControllerUtilsTrait {
  private val dmConfigKey = "eccencaDataManager.baseUrl"
  private val dmLinksKey = "eccencaDataManager.moduleLinks"
  private val hotkeyConfigPath = "frontend.hotkeys"
  private val dmLinkPath = "path"
  private val dmLinkIcon = "icon"
  private val dmLinkDefaultLabel = "defaultLabel"
  private val playMaxFileUploadSizeKey = "play.http.parser.maxDiskBuffer"
  private val versionKey = "workbench.version"
  private lazy val cfg = DefaultConfig.instance()
  private val log: Logger = Logger.getLogger(getClass.getName)

  lazy val dmBaseUrl: Option[JsString] = {
    if(cfg.hasPath(dmConfigKey)) {
      Some(JsString(cfg.getString(dmConfigKey)))
    } else {
      None
    }
  }

  lazy val version: Option[JsString] = {
    if(cfg.hasPath(versionKey)) {
      Some(JsString(cfg.getString(versionKey)))
    } else {
      None
    }
  }

  @Operation(
    summary = "Init frontend",
    description = "Returns information that is necessary for the frontend initialization or otherwise needed from the beginning on.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The `emptyWorkspace` parameter signals if the workspace is empty or contains at least one project. The `initialLangauge` parameter returns the initial language (either 'de' or 'en') that has been extracted from the Accept-language HTTP header send by the browser. The `maxFileUploadSize` specifies the max. file size in bytes. The `dmBaseUrl` is optional and returns the base URL, if configured in the DI config via parameter eccencaDataManager.baseUrl. The `dmModuleLinks` are only available if the DM base URL is defined. These are configured links to DM modules.",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(InitApiDoc.initFrontendExample)))
        )
      )
    ))
  def init(): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val emptyWorkspace = workspace.projects.isEmpty
    val resultJson = Json.obj(
      "emptyWorkspace" -> emptyWorkspace,
      "initialLanguage" -> initialLanguage(request),
      "hotKeys" -> Json.toJson(hotkeys()),
      "maxFileUploadSize" -> maxUploadSize,
      "templatingEnabled" -> GlobalTemplateVariablesConfig.isEnabled
    )
    val withDmUrl = dmBaseUrl.map { url =>
      resultJson + ("dmBaseUrl" -> url) + ("dmModuleLinks" -> JsArray(dmLinks.map(Json.toJson(_))))
    }.getOrElse(resultJson)
    val withVersion = version.map(v => withDmUrl + ("version" -> v)).getOrElse(withDmUrl)
    val withUser = userContext.user.map(user => withVersion + ("userUri" -> JsString(user.uri))).getOrElse(withVersion)
    Ok(withUser)
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


