package org.silkframework.rule

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.entity.{StringValueType, ValueType}
import org.silkframework.runtime.serialization.{ReadContext, TestReadContext, XmlSerialization}
import TransformRule.TransformRuleFormat
import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.UntypedPath

class TransformRuleXmlSerializationTest extends FlatSpec with Matchers {

  behavior of "TransformRule.XmlFormat"

  it should "serialize direct mappings" in {
    testSerialzation(DirectMapping("directMapping", UntypedPath("inputPath"), MappingTarget("outputProperty", ValueType.STRING)))
  }

  it should "serialize object mappings" in {
    testSerialzation(
      ObjectMapping(
        id = "objectMapping",
        sourcePath = UntypedPath("relativePath"),
        target = Some(MappingTarget("targetProperty")),
        rules = MappingRules(
          DirectMapping("directMapping", UntypedPath("inputPath"), MappingTarget("outputProperty", ValueType.STRING))
        )
      )
    )
  }

  def testSerialzation(obj: TransformRule): Unit = {
    implicit val readContext: ReadContext = TestReadContext()
    val xml = XmlSerialization.toXml(obj)
    val deserializedObj = XmlSerialization.fromXml[TransformRule](xml)

    deserializedObj shouldBe obj
  }

}
