package org.silkframework.serialization.json

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.plugin.PluginDescription
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.libs.json.{JsArray, JsObject, JsValue}
import JsonHelpers._
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.serialization.WriteContext
import JsonSerializers._

class PluginSerializersTest extends FlatSpec with MustMatchers {
  behavior of "Plugin Serializers"
  private final val PROPERTIES = "properties"

  it should "serialize schema of plugins with object parameters without schema correctly" in {
    serialize(classOf[Workflow], overviewOnly = true)._1.keys mustBe Set("title", "categories", "description")
    val (js, pd) = serialize(classOf[Workflow], taskType = Some("W"))
    stringValue(js, "title") mustBe pd.label
    stringValue(js, "type") mustBe "object"
    stringValue(js, TASKTYPE) mustBe "W"
    val properties = objectValue(js, PROPERTIES)
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
    stringValue(js, "pluginId") mustBe "transform"
    stringValueOption(js, TASKTYPE) mustBe empty
    val properties = objectValue(js, PROPERTIES)
    properties.keys mustBe Set("selection", "mappingRule", "output", "errorOutput", "targetVocabularies")
    mustBeJsObject(properties.value("selection")) { operatorsParam =>
      stringValue(operatorsParam, "type") mustBe "object"
      stringValue(operatorsParam, "pluginId") mustBe "datasetSelectionParameter"
      booleanValue(operatorsParam, "visibleInDialog") mustBe true
      val properties = objectValue(operatorsParam, PROPERTIES).value
      properties.keySet mustBe Set("inputId", "typeUri", "restriction")
      val inputIdObj = properties("inputId")
      stringValue(inputIdObj, TYPE) mustBe "string"
      optionalValue(inputIdObj, "autoCompletion") mustBe defined
    }
  }

  private def serialize(pluginClass: Class[_], markdown: Boolean = false, overviewOnly: Boolean = false, taskType: Option[String] = None): (JsObject, PluginDescription[_]) = {
    val pluginDescription = PluginDescription(pluginClass)
    implicit val writeContext: WriteContext[JsValue] = WriteContext[JsValue]()
    val json = PluginSerializers.PluginListJsonFormat.serializePlugin(pluginDescription, markdown, overviewOnly, taskType)
    (json, pluginDescription)
  }
}
