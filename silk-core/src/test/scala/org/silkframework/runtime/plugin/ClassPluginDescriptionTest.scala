package org.silkframework.runtime.plugin

import org.scalatest.{FlatSpec, MustMatchers}

class ClassPluginDescriptionTest extends FlatSpec with MustMatchers {

  behavior of "PluginDescription"

  private implicit val pluginContext: PluginContext = PluginContext.empty
  private val pluginDesc = ClassPluginDescription(classOf[TestPlugin])

  it should "create plugin instances with provided parameter values" in {
    val plugin = create("param1"-> "overridden default value", "param2" -> "123")
    plugin.param1 mustBe "overridden default value"
    plugin.param2 mustBe 123
  }


  it should "throw an exception if required parameter values are missing" in {
    intercept[InvalidPluginParameterValueException] {
      create("param1"-> "overriding default value")
    }
  }

  it should "throw an exception if a parameter value of the wrong type is provided" in {
    intercept[InvalidPluginParameterValueException] {
      create("param2"-> "no integer")
    }
  }

  it should "throw an exception if a parameter value for a parameter that does not exist is provided" in {
    intercept[InvalidPluginParameterValueException] {
      createNoIgnore("param2" -> "123", "param3"-> "some value")
    }
  }

  private def create(elems: (String, String)*): TestPlugin  = {
    pluginDesc(ParameterValues.fromStringMap(Map(elems: _*)))
  }

  private def createNoIgnore(elems: (String, String)*): TestPlugin = {
    pluginDesc(ParameterValues.fromStringMap(Map(elems: _*)), ignoreNonExistingParameters = false)
  }

}
