package org.silkframework.rule

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.util.Uri

/**
  * Created on 11/22/16.
  */
class MappingTargetTest extends FlatSpec with MustMatchers {
  behavior of "MatchingTarget"

  val mappingTarget = MappingTarget(propertyUri = Uri("http://uri"), valueType = FloatValueType)
  val mappingTargetCustom = MappingTarget(propertyUri = Uri("http://uri"), valueType = CustomValueType("http://RichString"))

  it should "serialize and deserialize to/from XML" in {
    implicit val readContext = ReadContext()
    val mappingTargetRoundTrip = XmlSerialization.fromXml[MappingTarget](XmlSerialization.toXml(mappingTarget))
    val mappingTargetCustomRoundTrip = XmlSerialization.fromXml[MappingTarget](XmlSerialization.toXml(mappingTargetCustom))
    mappingTarget mustBe mappingTargetRoundTrip
    mappingTargetCustom mustBe mappingTargetCustomRoundTrip
  }
}
