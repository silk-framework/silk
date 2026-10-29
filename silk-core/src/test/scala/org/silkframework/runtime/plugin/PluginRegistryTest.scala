package org.silkframework.runtime.plugin


import org.silkframework.runtime.plugin.StringParameterType.StringType
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginType}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class PluginRegistryTest extends AnyFlatSpec with Matchers {

  behavior of "PluginRegistry"

  it should "return plugin descriptions for registered classes" in  {
    PluginRegistry.pluginDescription(classOf[TestPlugin]) mustBe None
    PluginRegistry.registerPlugin(classOf[TestPlugin])
    PluginRegistry.pluginDescription(classOf[TestPlugin]) mustBe Symbol("defined")

    PluginRegistry.unregisterPlugin(classOf[TestPlugin])
    PluginRegistry.pluginDescription(classOf[TestPlugin]) mustBe None
  }

  it should "fail registering classes with invisible parameters having no default value" in {
    intercept[InvalidPluginException] {
      PluginRegistry.registerPlugin(classOf[InvalidInvisiblePluginParameterClass])
    }
  }

  it should "check object plugin parameters for validity" in {
    val visibleParameter = ClassPluginParameter("p", StringType, "p", advanced = false, visibleInDialog = true, autoCompletion = None)
    val invisibleParameter = ClassPluginParameter("p", StringType, "p", advanced = false, visibleInDialog = false, autoCompletion = None)
    // Check nesting
    PluginRegistry.checkInvalidObjectPluginParameterType(classOf[TestObjectParameter], Seq(visibleParameter)) mustBe defined
    PluginRegistry.checkInvalidObjectPluginParameterType(classOf[TestObjectParameterInner], Seq(visibleParameter)) mustBe empty
    // A parameter that is not shown in the UI can be nested arbitrarily since it does not need to be edited in the generic plugin dialog
    PluginRegistry.checkInvalidObjectPluginParameterType(classOf[TestObjectParameter], Seq(invisibleParameter)) mustBe empty
  }

  // Looking a plugin up under the wrong base type is the common mix-up, and "not found" hides it.
  it should "name the type an id is registered for when it is looked up under another one" in {
    PluginRegistry.registerPlugin(classOf[OtherTypeTestPlugin])
    try {
      val ex = intercept[NoSuchElementException] {
        PluginRegistry.pluginById[TestPluginType]("otherTypeTestPlugin")
      }
      ex.getMessage must include("otherTypeTestPlugin")
      ex.getMessage must include("Other test plugin type")
      ex.getMessage must include("TestPluginType")
    } finally {
      PluginRegistry.unregisterPlugin(classOf[OtherTypeTestPlugin])
    }
  }

  // The full list of a type can be hundreds of ids; the listing endpoints exist for that.
  it should "suggest the closest ids instead of the whole plugin type" in {
    val available = (1 to 50).map(i => s"plugin$i") :+ "subtract"
    val closest = PluginRegistry.closestPluginIds("subtrac", available)
    closest must include("Closest ids")
    closest must include("subtract")
    closest must not include "plugin42"

    // Nothing close: the ids are still capped and the total is named.
    val capped = PluginRegistry.closestPluginIds("zzz", available)
    capped must include("Available ids")
    capped must include("51 plugins are available for this type")
    capped.split(", ").length mustBe 10

    PluginRegistry.closestPluginIds("anything", Seq.empty) mustBe "No plugins are available for this type."
  }
}

@PluginType()
class OtherTestPluginType extends AnyPlugin

@Plugin(id = "otherTypeTestPlugin", label = "Other type test plugin")
case class OtherTypeTestPlugin() extends OtherTestPluginType

case class TestObjectParameter(param1: String,
                               param2: TestObjectParameterInner) extends PluginObjectParameter

case class TestObjectParameterInner(param: Int) extends PluginObjectParameter

@Plugin(id = "invalid", label = "invalid")
case class InvalidInvisiblePluginParameterClass(@Param(value = "invisible", visibleInDialog = false)
                                                invisible: String) extends TestPluginType
