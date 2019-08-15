package integration.test

import helper.IntegrationTestTrait
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.CustomTask
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.util.ConfigTestTrait

class BlacklistPluginsTest extends FlatSpec with MustMatchers with ConfigTestTrait with IntegrationTestTrait {
  behavior of "Plugin blacklist parameter"

  override def workspaceProviderId: String = "inMemory"

  override def propertyMap: Map[String, Option[String]] = Map(
    "plugin.blacklist" -> Some(" sparqlSelectOperator , xsltOperator ")
  )

  it should "blacklist all plugins in the list" in {
    val plugins = PluginRegistry.availablePlugins[CustomTask]
    val pluginIds = plugins.map(_.id.toString)
    pluginIds must contain noneOf ("sparqlSelectOperator", "xsltOperator")
    pluginIds must contain ("XmlParserOperator")
  }
}
