package org.silkframework.rule

import org.scalatest.{FlatSpec, ShouldMatchers}
import org.silkframework.entity.{Path, StringValueType}
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import TransformRule.TransformRuleFormat

class TransformRuleXmlSerializationTest extends FlatSpec with ShouldMatchers {

  behavior of "TransformRule.XmlFormat"

  it should "serialize direct mappings" in {
    testSerialzation(DirectMapping("directMapping", Path("inputPath"), MappingTarget("outputProperty", StringValueType)))
  }

  it should "serialize hierarchical mappings" in {
    testSerialzation(
      HierarchicalMapping(
        id = "hierarchicalMapping",
        sourcePath = Path("relativePath"),
        targetProperty = Some("targetProperty"),
        rules = MappingRules(
          DirectMapping("directMapping", Path("inputPath"), MappingTarget("outputProperty", StringValueType))
        )
      ))
  }

  def testSerialzation(obj: TransformRule): Unit = {
    implicit val readContext = ReadContext()
    val xml = XmlSerialization.toXml(obj)
    val deserializedObj = XmlSerialization.fromXml[TransformRule](xml)

    deserializedObj shouldBe obj
  }

}
