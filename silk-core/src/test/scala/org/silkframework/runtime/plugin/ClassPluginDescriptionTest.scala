package org.silkframework.runtime.plugin

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.runtime.plugin.annotations.{Action, Plugin}

class ClassPluginDescriptionTest extends AnyFlatSpec with Matchers {

  behavior of "PluginDescription"

  private implicit val pluginContext: PluginContext = TestPluginContext()
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

  it should "resolve each action to its own method even if two actions carry equal annotations" in {
    val desc = ClassPluginDescription(classOf[TwoActionsPlugin])
    val plugin = TwoActionsPlugin()
    desc.actions("actionA").apply(plugin) mustBe Some("resultA")
    desc.actions("actionB").apply(plugin) mustBe Some("resultB")
  }

  private def create(elems: (String, String)*): TestPlugin  = {
    pluginDesc(ParameterValues.fromStringMap(Map(elems: _*)))
  }

  private def createNoIgnore(elems: (String, String)*): TestPlugin = {
    pluginDesc(ParameterValues.fromStringMap(Map(elems: _*)), ignoreNonExistingParameters = false)
  }

}

// Two action methods with identical @Action annotations, which compare by value
@Plugin(id = "twoActionsPlugin", label = "Two actions")
case class TwoActionsPlugin() extends TestPluginType {
  @Action(label = "same", description = "same")
  def actionA(): String = "resultA"

  @Action(label = "same", description = "same")
  def actionB(): String = "resultB"
}
