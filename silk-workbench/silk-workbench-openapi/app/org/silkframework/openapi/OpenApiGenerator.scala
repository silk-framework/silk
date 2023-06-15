package org.silkframework.openapi

import config.WorkbenchConfig
import io.aurora.utils.play.swagger.SwaggerPlugin
import io.swagger.v3.core.util.{Json, Yaml}
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import io.swagger.v3.oas.models.{OpenAPI, PathItem, Paths}

import java.util
import java.util.logging.{Level, Logger}
import scala.io.Source
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.util.control.NonFatal

/**
  * Generates OpenAPI documentation.
  */
object OpenApiGenerator {

  private val descriptionFile = "description.md"

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  def generateJson(swaggerPlugin: SwaggerPlugin): String = {
    serializeJson(generate(swaggerPlugin))
  }

  def generateYaml(swaggerPlugin: SwaggerPlugin): String = {
    serializeYaml(generate(swaggerPlugin))
  }

  def generate(swaggerPlugin: SwaggerPlugin): OpenAPI = {
    val openApi = swaggerPlugin.apiListingCache.listing(WorkbenchConfig.publicHost)
    updateMetadata(openApi)
    updateDescription(openApi)
    sortTags(openApi)
    sortPaths(openApi)
    openApi
  }

  /**
    * Overwrites metadata, such as the version and the server path.
    */
  private def updateMetadata(openApi: OpenAPI): Unit = {
    openApi.getInfo.setVersion(WorkbenchConfig.version)
    val servers = new util.ArrayList[Server]()
    val server = new Server()
    server.setUrl(WorkbenchConfig.publicBaseUrl + WorkbenchConfig.applicationContext + "/")
    servers.add(server)
    openApi.setServers(servers)
  }

  /**
    * Loads the markdown documentation into the description.
    */
  private def updateDescription(openApi: OpenAPI): Unit = {
    val source = Source.fromResource(descriptionFile)
    try {
      openApi.getInfo.setDescription(source.mkString)
    } finally {
      source.close()
    }
  }

  /**
    * Sorts tags alphabetically (ignores trailing 's').
    */
  private def sortTags(openApi: OpenAPI): Unit = {
    openApi.getTags.sort((t1: Tag, t2: Tag) => {
      val name1 = t1.getName.stripSuffix("s")
      val name2 = t2.getName.stripSuffix("s")
      name1.compareTo(name2)
    })
  }

  /**
    * At the moment, the sorting of the paths returned by the SwaggerPlugin is too random.
    * Thus, we use our own sorting.
    */
  private def sortPaths(openApi: OpenAPI): Unit = {
    val sortedPaths = new Paths()
    for((key, value) <- openApi.getPaths.asScala.toSeq.sortWith(comparePaths)) {
      sortedPaths.addPathItem(key, value)
    }
    openApi.setPaths(sortedPaths)
  }

  private def comparePaths(v1: (String, PathItem), v2: (String, PathItem)): Boolean = {
    comparePaths(v1._1, v2._1)
  }

  def comparePaths(path1: String, path2: String): Boolean = {
    normalizePath(path1).compareTo(normalizePath(path2)) < 0
  }

  @inline
  private def normalizePath(path: String): String = {
    path.stripPrefix("/api") // Ignore the /api prefix so that new and old endpoints are ordered next to each other
        .replace('/', '~') // Slashes should be ordered after all alphanumerical characters
  }

  private def serializeJson(openApi: OpenAPI): String = {
    try {
      Json.pretty().writeValueAsString(openApi)
    } catch {
      case NonFatal(t) =>
        log.log(Level.WARNING, "Issue with generating OpenAPI documentation", t)
        throw t
    }
  }

  private def serializeYaml(openApi: OpenAPI): String = {
    try {
      Yaml.pretty().writeValueAsString(openApi)
    } catch {
      case NonFatal(t) =>
        log.log(Level.WARNING, "Issue with generating OpenAPI documentation", t)
        throw t
    }
  }

}