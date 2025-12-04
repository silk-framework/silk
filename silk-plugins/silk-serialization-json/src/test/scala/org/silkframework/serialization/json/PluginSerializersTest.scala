package org.silkframework.serialization.json


import org.silkframework.runtime.plugin.{AnyPlugin, ClassPluginDescription, PluginDescription}
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.libs.json.{JsArray, JsObject, JsValue}
import JsonHelpers._
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.serialization.{TestWriteContext, WriteContext}
import JsonSerializers._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class PluginSerializersTest extends AnyFlatSpec with Matchers {
  behavior of "Plugin Serializers"
  private final val PROPERTIES = "properties"

  it should "serialize schema of plugins with object parameters without schema correctly" in {
    serialize(classOf[Workflow], overviewOnly = true)._1.keys mustBe Set("title", "categories", "description", "backendType", "actions")
    val (js, pd) = serialize(classOf[Workflow], taskType = Some("W"))
    stringValue(js, "title") mustBe pd.label
    stringValue(js, "type") mustBe "object"
    stringValue(js, TASKTYPE) mustBe "W"
    val properties = objectValue(js, PROPERTIES)
    properties.keys mustBe Set("operators", "datasets", "uiAnnotations", "replaceableInputs", "replaceableOutputs")
    mustBeJsObject(properties.value("operators")) { operatorsParam =>
      stringValue(operatorsParam, "type") mustBe "object"
      arrayValueOption(operatorsParam, "value") mustBe Some(JsArray())
      booleanValue(operatorsParam, "visibleInDialog") mustBe false
    }
  }

  it should "serialize schema of plugins with object parameters with schema correctly" in {
    serialize(classOf[TransformSpec], overviewOnly = true)._1.keys mustBe Set("title", "categories", "description", "backendType", "actions")
    val (js, pd) = serialize(classOf[TransformSpec])
    stringValue(js, "title") mustBe pd.label
    stringValue(js, "type") mustBe "object"
    stringValue(js, "pluginId") mustBe "transform"
    stringValueOption(js, TASKTYPE) mustBe empty
    val properties = objectValue(js, PROPERTIES)
    properties.keys mustBe Set("selection", "mappingRule", "output", "errorOutput", "targetVocabularies", "abortIfErrorsOccur")
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

  private def serialize(pluginClass: Class[_ <: AnyPlugin],
                        markdown: Boolean = false,
                        overviewOnly: Boolean = false,
                        taskType: Option[String] = None): (JsObject, PluginDescription[_]) = {
    val pluginDescription = ClassPluginDescription(pluginClass)
    implicit val writeContext: WriteContext[JsValue] = TestWriteContext[JsValue]()
    val json = PluginDescriptionSerializers.PluginListJsonFormat.serializePlugin(pluginDescription, markdown, overviewOnly, taskType, withLabels = false)
    (json, pluginDescription)
  }
}
