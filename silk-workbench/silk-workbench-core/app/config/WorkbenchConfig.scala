package config

import com.typesafe.config.{Config => TypesafeConfig}
import config.WorkbenchConfig.Tabs
import org.silkframework.config.{Config, DefaultConfig}
import org.silkframework.runtime.metrics.MeterRegistryProvider
import org.silkframework.runtime.metrics.MetricsConfig.prefix
import org.silkframework.runtime.resource._
import play.api.libs.json.JsString
import play.api.mvc.RequestHeader
import play.api.{Configuration, Environment, Mode}
import play.twirl.api.Html

import java.io.{File, InputStream}
import java.util.Base64
import javax.inject.Inject
import scala.io.{Codec, Source}
import scala.util.{Failure, Success, Try}

/**
 * Workbench configuration.
 *
 * @param title The application title.
 * @param logo The application logo. Must point to a file in the conf directory.
 * @param logoSmall Small version of the application logo. Must point to a file in the conf directory.
 * @param welcome Welcome message. Must point to a file in the conf directory.
 * @param tabs The shown tabs.
 */
case class WorkbenchConfig(title: String = "Silk Workbench",
                           version: String,
                           logo: Resource,
                           logoSmall: Resource,
                           welcome: Resource,
                           about: Resource,
                           mdlStyle: Option[Resource],
                           tabs: Tabs = Tabs(),
                           protocol: String,
                           loggedOut: Resource) {
  var showLogoutButton: Boolean = false
  val useHttps: Boolean = protocol == "https"

  def showHeader(request: RequestHeader): Boolean = {
    !request.queryString.get("inlineView").exists(_.exists(_.toLowerCase == "true"))
  }

}

object WorkbenchConfig {
  private lazy val cfg = DefaultConfig.instance()
  private val defaultProjectPageSuffixKey = "workbench.project.defaultUrlSuffix"
  lazy val publicProtocol: String = if(WorkbenchConfig.useHttps(cfg)) "https" else "http"
  lazy val publicHost: String = WorkbenchConfig.host(cfg).getOrElse("localhost:9000")
  lazy val publicBaseUrl: String = s"$publicProtocol://$publicHost"
  lazy val applicationContext: String = WorkbenchConfig.applicationContext(cfg)
  lazy val defaultProjectPageSuffix: Option[String] = {
    if(cfg.hasPath(defaultProjectPageSuffixKey)) {
      Some(cfg.getString(defaultProjectPageSuffixKey))
    } else {
      None
    }
  }

  /** The public host name and port of the server this application runs on. */
  def host(config: TypesafeConfig): Option[String] = {
    if (config.hasPath("workbench.host")) {
      Some(config.getString("workbench.host"))
    } else {
      None
    }
  }

  /** SSL enabled for public address. */
  def useHttps(config: TypesafeConfig): Boolean = {
    if (config.hasPath("workbench.protocol")) {
      config.getString("workbench.protocol") == "https"
    } else {
      false
    }
  }

  /** The application context, i.e. the base path of the absolute application paths. */
  def applicationContext(config: TypesafeConfig): String = {
    if (config.hasPath("play.http.context")) {
      config.getString("play.http.context").stripSuffix("/")
    } else {
      ""
    }
  }

  @javax.inject.Singleton
  class WorkspaceReact @Inject()(env: Environment) {
    private lazy val _indexHtml: Html = calculateHtml()
    private lazy val _styleLinks: Seq[String] = calculateStyleLinks()
    private lazy val _jsLinks: Seq[String] = calculateJsLinks()

    def indexHtml: Html = {
      if(env.mode == Mode.Prod) {
        _indexHtml
      } else {
        calculateHtml()
      }
    }

    /** Returns the absolute paths to the style (CSS) files. */
    def styleLinks: Seq[String] = {
      if(env.mode == Mode.Prod) {
        _styleLinks
      } else {
        calculateStyleLinks()
      }
    }

    /** Returns the absolute paths to the style (CSS) files. */
    def jsLinks: Seq[String] = {
      if(env.mode == Mode.Prod) {
        _jsLinks
      } else {
        calculateJsLinks()
      }
    }

    def getClassLoaderResource(resource: String): InputStream = this.getClass.getClassLoader.getResourceAsStream(resource)

    private def calculateHtml(): Html = {
      val context = WorkbenchConfig.applicationContext
      val source = Source.fromInputStream(getClassLoaderResource("public/index.html"))(Codec.UTF8)
      val htmlString = source.getLines().mkString("\n")
      source.close()
      val html = injectConfigProperties(context, htmlString)
      val rewrittenHtml = adaptUrls(context, html)
      Html(rewrittenHtml)
    }

    private def calculateStyleLinks(): Seq[String] = {
      val htmlString = indexHtml.toString()
      val regex = """(<link\s+[^>]*rel="stylesheet"\s*[^>]*>)""".r
      val links = regex.findAllMatchIn(htmlString) flatMap { m =>
        val linkString = htmlString.substring(m.start(1), m.end(1))
        val linkHrefRegex = """href="([^"]+)"""".r
        linkHrefRegex.findFirstMatchIn(linkString).map(m => linkString.substring(m.start(1), m.end(1)))
      }
      links.toSeq
    }

    private def calculateJsLinks(): Seq[String] = {
      val htmlString = indexHtml.toString()
      val regex = """(<script\s+[^>]*src\s*[^>]*>)""".r
      val links = regex.findAllMatchIn(htmlString) flatMap { m =>
        val linkString = htmlString.substring(m.start(1), m.end(1))
        val linkSrcRegex = """src="([^"]+)"""".r
        linkSrcRegex.findFirstMatchIn(linkString).map(m => linkString.substring(m.start(1), m.end(1)))
      }
      links.toSeq
    }
  }

  private def adaptUrls(context: String, html: String): String = {
    if (context != "") {
      val regex = """(?:src|href)=\"([^"]+)\"""".r
      val sb = new StringBuilder()
      var lastEnd = 0
      for (m <- regex.findAllMatchIn(html)) {
        val start = m.start(1)
        val end = m.end(1)
        sb.append(html.substring(lastEnd, start))
        sb.append(context + html.substring(start, end))
        lastEnd = end
      }
      sb.append(html.substring(lastEnd))
      sb.toString()
    } else {
      html
    }

  }

  /** Injects config properties that are needed by the frontend, e.g. context path. */
  private def injectConfigProperties(context: String, htmlString: String): String = {
    val htmlParts = htmlString.split("<head>")
    assert(htmlParts.size == 2, "The index.html does not have the required format to be parsed correctly.")
    val scriptPart = s"""<script>window.DI = {"basePath": "$context", "publicBaseUrl":"${WorkbenchConfig.publicBaseUrl}"}</script>"""
    val html = s"${htmlParts(0)}<head>$scriptPart${htmlParts(1)}"
    html
  }

  // The version of the workbench
  lazy val version: String = {
    Try(
      DefaultConfig.instance.apply().getString("workbench.version")
    ) match {
      case Success(versionString) =>
        // Register the version string in a Micrometer counter.
        // The counter is not increased further. What's important, is the version string set in the value of the tag.
        // This is arguably a somewhat improper or quirky usage of Micrometer and metrics in general.
        MeterRegistryProvider.meterRegistry.counter(s"$prefix.workbench.config", "version", versionString).increment()
        versionString
      case Failure(_) =>
        throw new RuntimeException("No version string ist set!")
    }
  }

  /**
   * Retrieves the Workbench configuration.
   */
  lazy val get = {
    val config = Configuration(DefaultConfig.instance())
    val resourceLoader = getResourceLoader

    WorkbenchConfig(
      title = config.getOptional[String]("workbench.title").getOrElse("Silk Workbench"),
      version = version,
      logo = resourceLoader.get(config.getOptional[String]("workbench.logo").getOrElse("logo.png")),
      logoSmall = loadIcon(resourceLoader, config.getOptional[String]("workbench.logoSmall").getOrElse("logo.png")),
      welcome = resourceLoader.get(config.getOptional[String]("workbench.welcome").getOrElse("welcome.html")),
      about = resourceLoader.get(config.getOptional[String]("workbench.about").getOrElse("about.html")),
      mdlStyle = config.getOptional[String]("workbench.mdlStyle").map(r=>resourceLoader.get(r)),
      tabs = Tabs(
               config.getOptional[Boolean]("workbench.tabs.editor").getOrElse(true),
               config.getOptional[Boolean]("workbench.tabs.generateLinks").getOrElse(true),
               config.getOptional[Boolean]("workbench.tabs.learn").getOrElse(true),
               config.getOptional[Boolean]("workbench.tabs.referenceLinks").getOrElse(true),
               config.getOptional[Boolean]("workbench.tabs.status").getOrElse(true),
               config.getOptional[Boolean]("workbench.tabs.legacyWorkflowEditor").getOrElse(true)
             ),
      protocol = config.getOptional[String]("workbench.protocol").getOrElse("http"),
      loggedOut = resourceLoader.get("loggedOut.html")
    )
  }

  def apply(): WorkbenchConfig = get

  /**
   * Loads an icon resource, which can either be a file path or a data URL.
   */
  private def loadIcon(resourceLoader: ResourceLoader, name: String): Resource = {
    if(name.startsWith("data:")) {
      // Parse the data URL to extract the Base64 encoded data
      val commaIndex = name.indexOf(',')
      if (commaIndex == -1) throw new IllegalArgumentException("Invalid data URL: does not contain a comma.")
      val base64Data = name.substring(commaIndex + 1)
      // Decode the Base64 string into a byte array
      val decodedBytes = Base64.getDecoder.decode(base64Data)
      // Create an InMemoryResource with the decoded bytes
      new InMemoryResource(name, name, decodedBytes)
    } else {
      resourceLoader.get(name)
    }
  }

  def getResourceLoader: ResourceLoader = {
    DefaultConfig.instance.eldsHomeDir match {
      case None =>
        // If no eLDs home directory is set, use the classpath resource loader only.
        ClasspathResourceLoader("")
      case Some(eldsHome) =>
        // If an eLDs home directory is set, use the file resource manager for the config directory.
        val configDir = new File(eldsHome, Config.DATAINTEGRATION_CONFIG_DIR)
        FallbackResourceManager(ReadOnlyResourceManager(ClasspathResourceLoader("")), FileResourceManager(configDir), writeIntoFallbackLoader = false)
    }
  }

  /**
   * Controls which tabs are shown.
   */
  case class Tabs(editor: Boolean = true,
                  generateLinks: Boolean = true,
                  learn: Boolean = true,
                  referenceLinks:Boolean = true,
                  status: Boolean = true,
                  legacyWorkflowEditor: Boolean = true)
}
