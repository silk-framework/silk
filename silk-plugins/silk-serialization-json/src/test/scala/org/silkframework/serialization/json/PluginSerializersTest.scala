package org.silkframework.serialization.json

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.plugin.PluginDescription
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.libs.json.{JsArray, JsObject}
import JsonHelpers._
import org.silkframework.rule.TransformSpec

class PluginSerializersTest extends FlatSpec with MustMatchers {
  behavior of "Plugin Serializers"

  it should "serialize schema of plugins with object parameters without schema correctly" in {
    serialize(classOf[Workflow], overviewOnly = true)._1.keys mustBe Set("title", "categories", "description")
    val (js, pd) = serialize(classOf[Workflow], taskType = Some("W"))
    stringValue(js, "title") mustBe pd.label
    stringValue(js, "type") mustBe "object"
    stringValue(js, JsonSerializers.TASKTYPE) mustBe "W"
    val properties = objectValue(js, "properties")
    properties.keys mustBe Set("operators", "datasets")
    mustBeJsObject(properties.value("operators")) { operatorsParam =>
      stringValue(operatorsParam, "type") mustBe "object"
      arrayValueOption(operatorsParam, "value") mustBe Some(JsArray())
      booleanValue(operatorsParam, "visibleInDialog") mustBe false
    }
  }

  it should "serialize schema of plugins with object parameters with schema correctly" in {
    serialize(classOf[TransformSpec], overviewOnly = true)._1.keys mustBe Set("title", "categories", "description")
    val (js, pd) = serialize(classOf[TransformSpec])
    stringValue(js, "title") mustBe pd.label
    stringValue(js, "type") mustBe "object"
    stringValueOption(js, JsonSerializers.TASKTYPE) mustBe empty
    val properties = objectValue(js, "properties")
    properties.keys mustBe Set("selection", "mappingRule", "outputOpt", "errorOutputOpt", "targetVocabularies")
    mustBeJsObject(properties.value("selection")) { operatorsParam =>
      stringValue(operatorsParam, "type") mustBe "object"
      booleanValue(operatorsParam, "visibleInDialog") mustBe true
      arrayValue(operatorsParam, JsonSerializers.PARAMETERS)
    }
  }

  private def serialize(pluginClass: Class[_], markdown: Boolean = false, overviewOnly: Boolean = false, taskType: Option[String] = None): (JsObject, PluginDescription[_]) = {
    val pluginDescription = PluginDescription(pluginClass)
    val json = PluginSerializers.PluginListJsonFormat.serializePlugin(pluginDescription, markdown, overviewOnly, taskType)
    (json, pluginDescription)
  }
}
