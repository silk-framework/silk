package org.silkframework.runtime.plugin

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.Prefixes

class PluginDescriptionTest extends FlatSpec with MustMatchers {

  behavior of "PluginDescription"

  private implicit val prefixes: Prefixes = Prefixes.empty
  private val pluginDesc = PluginDescription(classOf[TestPlugin])

  it should "create plugin instances with provided parameter values" in {
    val plugin = pluginDesc(Map("param1"-> "overridden default value", "param2" -> "123"))
    plugin.param1 mustBe "overridden default value"
    plugin.param2 mustBe 123
  }


  it should "throw an exception if required parameter values are missing" in {
    intercept[InvalidPluginParameterValueException] {
      pluginDesc(Map("param1"-> "overriding default value"))
    }
  }

  it should "throw an exception if a parameter value of the wrong type is provided" in {
    intercept[InvalidPluginParameterValueException] {
      pluginDesc(Map("param2"-> "no integer"))
    }
  }

  it should "throw an exception if a parameter value for a parameter that does not exist is provided" in {
    intercept[InvalidPluginParameterValueException] {
      pluginDesc(Map("param2" -> "123", "param3"-> "some value"), ignoreNonExistingParameters = false)
    }
  }

}
