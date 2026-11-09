package controllers.workspaceApi

import java.util.logging.Logger
import java.net.URI
import com.typesafe.config.{Config, ConfigValueType}
import config.WorkbenchConfig
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
import org.silkframework.runtime.users.UserActions
import org.silkframework.workspace.access.AccessControlConfig
import play.api.libs.json.{Format, JsArray, JsString, Json, OFormat}
import play.api.mvc.{Action, AnyContent, InjectedController, Request}

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try

/**
  * API endpoints for initialization of the frontend application.
  */
@Tag(name = "Workbench")
case class InitApi @Inject()() extends InjectedController with UserContextActions with ControllerUtilsTrait {
  private val dmLinksKey = "eccencaDataManager.moduleLinks"
  private val hotkeyConfigPath = "frontend.hotkeys"
  private val dmLinkPath = "path"
  private val dmLinkIcon = "icon"
  private val dmLinkDefaultLabel = "defaultLabel"
  private val playMaxFileUploadSizeKey = "play.http.parser.maxDiskBuffer"
  private val assistantConfigKey = "com.eccenca.di.assistant.ApiConfig"
  private val mappingCreatorEnabledKey = "com.eccenca.di.mappingCreatorEnabled"
  private val versionKey = "workbench.version"
  private lazy val cfg = DefaultConfig.instance()
  private val log: Logger = Logger.getLogger(getClass.getName)

  lazy val dmBaseUrl: Option[JsString] = WorkbenchConfig.dataManagerBaseUrl.map(JsString(_))

  lazy val version: Option[JsString] = {
    if(cfg.hasPath(versionKey)) {
      Some(JsString(cfg.getString(versionKey)))
    } else {
      None
    }
  }

  lazy val assistantSupported: Boolean = {
    cfg.hasPath(assistantConfigKey) && {
      val assistantCfg = cfg.getConfig(assistantConfigKey)
      (assistantCfg.hasPath("apiKey") && assistantCfg.getString("apiKey") != "") ||
        assistantCfg.hasPath("coreUrl") ||
        (assistantCfg.hasPath("useDataPlatformGateway") && assistantCfg.getBoolean("useDataPlatformGateway"))
    }
  }

  lazy val mappingCreatorEnabled: Boolean = {
    cfg.hasPath(mappingCreatorEnabledKey) && cfg.getBoolean(mappingCreatorEnabledKey)
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
    val emptyWorkspace = workspace.userProjects.isEmpty
    val resultJson = Json.obj(
      "emptyWorkspace" -> emptyWorkspace,
      "initialLanguage" -> initialLanguage(request),
      "hotKeys" -> Json.toJson(hotkeys()),
      "maxFileUploadSize" -> maxUploadSize,
      "templatingEnabled" -> GlobalTemplateVariablesConfig.isEnabled,
      "assistantSupported" -> assistantSupported,
      "companion" -> Json.toJson(companionConfig(userContext.user.map(_.actions).getOrElse(UserActions.empty))),
      "mappingCreatorEnabled" -> mappingCreatorEnabled,
      "aclEnabled" -> AccessControlConfig().enabled
    )
    val withDmUrl = dmBaseUrl.map { url =>
      resultJson + ("dmBaseUrl" -> url) + ("dmModuleLinks" -> JsArray(dmLinks.map(Json.toJson(_))))
    }.getOrElse(resultJson)
    val withVersion = version.map(v => withDmUrl + ("version" -> v)).getOrElse(withDmUrl)
    val withUser = userContext.user.map(user => withVersion + ("userUri" -> JsString(user.uri))).getOrElse(withVersion)
    val withDefaultProjectPageSuffix = WorkbenchConfig.defaultProjectPageSuffix.map(suffix => withUser + ("defaultProjectPageSuffix" -> JsString(suffix))).getOrElse(withUser)
    Ok(withDefaultProjectPageSuffix)
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

  private def companionConfig(userActions: UserActions): CompanionFrontendConfig = {
    CompanionFrontendConfig.fromConfig(cfg, userActions).fold(
      error => {
        log.warning(s"Invalid companion configuration: $error. The companion will be disabled.")
        CompanionFrontendConfig.disabled
      },
      identity
    )
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

/** Browser-facing companion configuration. URLs are constrained to same-origin paths. */
case class CompanionFrontendConfig(enabled: Boolean, apiBasePath: String, streamPath: String)

object CompanionFrontendConfig {
  private val configPath = "com.eccenca.di.assistant.CompanionConfig"
  private val companionAction = "https://vocab.eccenca.com/auth/Action/Explore-Companion-Use"
  private val defaultApiBasePath = "/dataplatform/api/companion"
  private val defaultStreamPath = "/dataplatform/companion-websocket"

  val disabled: CompanionFrontendConfig = CompanionFrontendConfig(
    enabled = false,
    apiBasePath = defaultApiBasePath,
    streamPath = defaultStreamPath
  )

  implicit val companionFrontendConfigFormat: OFormat[CompanionFrontendConfig] = Json.format[CompanionFrontendConfig]

  def fromConfig(config: Config, userActions: UserActions): Either[String, CompanionFrontendConfig] = {
    if(!config.hasPath(configPath)) {
      Right(disabled)
    } else {
      Try {
        val companionConfig = config.getConfig(configPath)
        val apiBasePath = configuredPath(companionConfig, "apiBasePath", defaultApiBasePath)
        val streamPath = configuredPath(companionConfig, "streamPath", defaultStreamPath)
        val invalidPaths = Seq(apiBasePath, streamPath).filterNot(isSameOriginAbsolutePath)
        if(invalidPaths.nonEmpty) {
          Left(s"Companion URLs must be same-origin absolute paths: ${invalidPaths.mkString(", ")}")
        } else {
          val installationEnabled = companionConfig.hasPath("enabled") && companionConfig.getBoolean("enabled")
          Right(CompanionFrontendConfig(
            enabled = installationEnabled && userActions.contains(companionAction),
            apiBasePath = apiBasePath,
            streamPath = streamPath
          ))
        }
      }.toEither.left.map(_.getMessage).flatMap(identity)
    }
  }

  private def configuredPath(config: Config, key: String, default: String): String = {
    if(config.hasPath(key)) config.getString(key) else default
  }

  private def isSameOriginAbsolutePath(value: String): Boolean = {
    Try(new URI(value)).toOption.exists { uri =>
      value.startsWith("/") &&
        !value.startsWith("//") &&
        !value.contains('\\') &&
        !uri.isAbsolute &&
        uri.getRawAuthority == null &&
        uri.getRawQuery == null &&
        uri.getRawFragment == null
    }
  }
}
