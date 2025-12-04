package integration.test

import helper.IntegrationTestTrait

import org.silkframework.config.CustomTask
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.util.ConfigTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class BlacklistPluginsTest extends AnyFlatSpec with Matchers with ConfigTestTrait with IntegrationTestTrait {
  behavior of "Plugin blacklist parameter"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

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
