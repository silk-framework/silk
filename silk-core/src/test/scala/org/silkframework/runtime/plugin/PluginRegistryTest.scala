package org.silkframework.runtime.plugin

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.plugin.StringParameterType.StringType
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

class PluginRegistryTest extends FlatSpec with MustMatchers {

  behavior of "PluginRegistry"

  it should "return plugin descriptions for registered classes" in  {
    PluginRegistry.pluginDescription(classOf[TestPlugin]) mustBe None
    PluginRegistry.registerPlugin(classOf[TestPlugin])
    PluginRegistry.pluginDescription(classOf[TestPlugin]) mustBe ('defined)
  }

  it should "fail registering classes with invisible parameters having no default value" in {
    intercept[InvalidPluginException] {
      PluginRegistry.registerPlugin(classOf[InvalidInvisiblePluginParameterClass])
    }
  }

  it should "check object plugin parameters for validity" in {
    val visibleParameter = Parameter("p", StringType, "p", advanced = false, visibleInDialog = true, autoCompletion = None)
    val invisibleParameter = Parameter("p", StringType, "p", advanced = false, visibleInDialog = false, autoCompletion = None)
    // Check nesting
    PluginRegistry.checkInvalidObjectPluginParameterType(classOf[TestObjectParameter], Seq(visibleParameter)) mustBe defined
    PluginRegistry.checkInvalidObjectPluginParameterType(classOf[TestObjectParameterInner], Seq(visibleParameter)) mustBe empty
    // A parameter that is not shown in the UI can be nested arbitrarily since it does not need to be edited in the generic plugin dialog
    PluginRegistry.checkInvalidObjectPluginParameterType(classOf[TestObjectParameter], Seq(invisibleParameter)) mustBe empty
  }
}

case class TestObjectParameter(param1: String,
                               param2: TestObjectParameterInner) extends PluginObjectParameter

case class TestObjectParameterInner(param: Int) extends PluginObjectParameter

@Plugin(id = "invalid", label = "invalid")
case class InvalidInvisiblePluginParameterClass(@Param(value = "invisible", visibleInDialog = false)
                                                invisible: String
)
