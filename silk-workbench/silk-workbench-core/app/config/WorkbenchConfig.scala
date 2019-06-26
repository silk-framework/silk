package config

import java.io.File

import config.WorkbenchConfig.Tabs
import org.silkframework.buildInfo.BuildInfo
import org.silkframework.config.DefaultConfig
import org.silkframework.runtime.resource._
import play.api.Configuration

import scala.util.{Failure, Success, Try}

/**
 * Workbench configuration.
 *
 * @param title The application title.
 * @param showHeader Whether the header is shown
 * @param logo The application logo. Must point to a file in the conf directory.
 * @param welcome Welcome message. Must point to a file in the conf directory.
 * @param tabs The shown tabs.
 */
case class WorkbenchConfig(title: String = "Silk Workbench",
                           version: String,
                           showHeader: Boolean,
                           logo: Resource,
                           welcome: Resource,
                           about: Resource,
                           mdlStyle: Option[Resource],
                           tabs: Tabs = Tabs(),
                           protocol: String,
                           loggedOut: Resource) {
  var showLogoutButton: Boolean = false
  val useHttps: Boolean = protocol == "https"
}

object WorkbenchConfig {
  // The version of the workbench
  lazy val version = {
    Try(
      DefaultConfig.instance.apply().getString("workbench.version")
    ) match {
      case Success(versionString) =>
        versionString
      case Failure(ex) =>
        BuildInfo.version
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
      showHeader = config.getOptional[Boolean]("workbench.showHeader").getOrElse(true),
      logo = resourceLoader.get(config.getOptional[String]("workbench.logo").getOrElse("logo.png")),
      welcome = resourceLoader.get(config.getOptional[String]("workbench.welcome").getOrElse("welcome.html")),
      about = resourceLoader.get(config.getOptional[String]("workbench.about").getOrElse("about.html")),
      mdlStyle = config.getOptional[String]("workbench.mdlStyle").map(r=>resourceLoader.get(r)),
      tabs = Tabs(
               config.getOptional[Boolean]("workbench.tabs.editor").getOrElse(true),
               config.getOptional[Boolean]("workbench.tabs.generateLinks").getOrElse(true),
               config.getOptional[Boolean]("workbench.tabs.learn").getOrElse(true),
               config.getOptional[Boolean]("workbench.tabs.referenceLinks").getOrElse(true),
               config.getOptional[Boolean]("workbench.tabs.status").getOrElse(true)
             ),
      protocol = config.getOptional[String]("workbench.protocol").getOrElse("http"),
      loggedOut = resourceLoader.get("loggedOut.html")
    )
  }

  def apply(): WorkbenchConfig = get

  def getResourceLoader: ResourceLoader = {
    //Depending on the distribution method, the configuration resources may be located at different locations.
    //We identify the configuration location by searching for the application configuration.
    if(new File("conf/application.conf").exists() || new File("conf/reference.conf").exists())
      FileResourceManager(new File("conf/"))
    else if(new File("silk-workbench/conf/application.conf").exists() || new File("silk-workbench/conf/reference.conf").exists())
      FileResourceManager(new File("silk-workbench/conf/"))
    else if(new File("../conf/application.conf").exists() || new File("../conf/reference.conf").exists())
      FileResourceManager(new File("../conf/"))
    else if(getClass.getClassLoader.getResourceAsStream("reference.conf") != null || getClass.getClassLoader.getResourceAsStream("application.conf") != null)
      ClasspathResourceLoader("")
    else
      throw new ResourceNotFoundException("Could not locate configuration. Current directory is: " + new File(".").getAbsolutePath)
  }

  /**
   * Controls which tabs are shown.
   */
  case class Tabs(editor: Boolean = true,
                  generateLinks: Boolean = true,
                  learn: Boolean = true,
                  referenceLinks:Boolean = true,
                  status: Boolean = true)
}
