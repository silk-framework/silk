package org.silkframework.rule


import org.silkframework.entity.{StringValueType, ValueType}
import org.silkframework.runtime.serialization.{ReadContext, TestReadContext, XmlSerialization}
import TransformRule.TransformRuleFormat
import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.input.{Input, PathInput}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TransformRuleXmlSerializationTest extends AnyFlatSpec with Matchers {

  behavior of "TransformRule.XmlFormat"

  it should "serialize direct mappings" in {
    testSerialzation(DirectMapping("directMapping", UntypedPath("inputPath"), MappingTarget("outputProperty", ValueType.STRING), inputId = Some("inputPath")))
  }

  it should "serialize object mappings" in {
    testSerialzation(
      ObjectMapping(
        id = "objectMapping",
        sourcePath = UntypedPath("relativePath"),
        target = Some(MappingTarget("targetProperty")),
        rules = MappingRules(
          DirectMapping("directMapping", UntypedPath("inputPath"), MappingTarget("outputProperty", ValueType.STRING), inputId = Some("inputPath"))
        )
      )
    )
  }

  it should "parse a non-self-closing Input element with whitespace content" in {
    implicit val readContext: ReadContext = TestReadContext()
    // Non-self-closing <Input> tags (e.g. from pretty-printed XML) must parse correctly
    val xml = <Input id="my_field" path="my_field">
    </Input>
    val input = Input.InputFormat.read(xml)
    input shouldBe PathInput(id = "my_field", path = UntypedPath("my_field"))
  }

  def testSerialzation(obj: TransformRule): Unit = {
    implicit val readContext: ReadContext = TestReadContext()
    val xml = XmlSerialization.toXml(obj)
    val deserializedObj = XmlSerialization.fromXml[TransformRule](xml)

    deserializedObj shouldBe obj
  }

}
