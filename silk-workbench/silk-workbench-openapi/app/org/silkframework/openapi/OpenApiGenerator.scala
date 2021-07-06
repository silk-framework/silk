package org.silkframework.openapi

import config.WorkbenchConfig
import io.aurora.utils.play.swagger.SwaggerPlugin
import io.swagger.v3.core.util.{Json, Yaml}
import io.swagger.v3.oas.models.{OpenAPI, PathItem, Paths}

import java.util.logging.{Level, Logger}
import scala.jdk.CollectionConverters.mapAsScalaMapConverter
import scala.util.control.NonFatal

/**
  * Generates OpenAPI documentation.
  */
object OpenApiGenerator {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  def generateJson(swaggerPlugin: SwaggerPlugin, host: String): String = {
    serializeJson(generate(swaggerPlugin, host))
  }

  def generateYaml(swaggerPlugin: SwaggerPlugin, host: String): String = {
    serializeYaml(generate(swaggerPlugin, host))
  }

  def generate(swaggerPlugin: SwaggerPlugin, host: String): OpenAPI = {
    val openApi = swaggerPlugin.apiListingCache.listing(host)
    openApi.getInfo.setVersion(WorkbenchConfig.version)
    sortPaths(openApi)
    openApi
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
    val path1 = v1._1
    val path2 = v2._1
    val namespace1 = namespace(path1)
    val namespace2 = namespace(path2)
    if(namespace1.startsWith(namespace2) || namespace2.startsWith(namespace1)) {
      if(namespace1.length == namespace2.length) {
        path1.compareTo(path2) < 0
      } else {
        namespace1.length < namespace2.length
      }
    } else {
      path1.compareTo(path2) < 0
    }
  }

  @inline
  private def namespace(path: String): String = {
    val endIndex = path.lastIndexOf('/')
    if(endIndex != -1) {
      path.substring(0, endIndex)
    } else {
      path
    }
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