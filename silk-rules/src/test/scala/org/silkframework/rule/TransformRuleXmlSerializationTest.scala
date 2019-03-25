package org.silkframework.rule

import org.scalatest.{FlatSpec, ShouldMatchers}
import org.silkframework.entity.{Path, StringValueType}
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import TransformRule.TransformRuleFormat
import org.silkframework.config.Prefixes

class TransformRuleXmlSerializationTest extends FlatSpec with ShouldMatchers {

  behavior of "TransformRule.XmlFormat"

  it should "serialize direct mappings" in {
    testSerialzation(DirectMapping("directMapping", Path("inputPath"), MappingTarget("outputProperty", StringValueType)))
  }

  it should "serialize object mappings" in {
    testSerialzation(
      ObjectMapping(
        id = "objectMapping",
        sourcePath = Path("relativePath"),
        target = Some(MappingTarget("targetProperty")),
        rules = MappingRules(
          DirectMapping("directMapping", Path("inputPath"), MappingTarget("outputProperty", StringValueType))
        )
      )
    )
  }

  def testSerialzation(obj: TransformRule): Unit = {
    implicit val readContext = ReadContext()
    val xml = XmlSerialization.toXml(obj)
    val deserializedObj = XmlSerialization.fromXml[TransformRule](xml)

    deserializedObj shouldBe obj
  }

}
