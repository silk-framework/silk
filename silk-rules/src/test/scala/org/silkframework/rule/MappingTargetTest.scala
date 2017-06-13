package org.silkframework.rule

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.{CustomValueType, FloatValueType, UriValueType}
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.util.Uri

/**
  * Created on 11/22/16.
  */
class MappingTargetTest extends FlatSpec with MustMatchers {
  behavior of "MatchingTarget"

  private val PROP_URI = "http://uri"
  val mappingTarget = MappingTarget(propertyUri = Uri(PROP_URI), valueType = FloatValueType)
  val mappingTargetCustom = MappingTarget(propertyUri = Uri(PROP_URI), valueType = CustomValueType("http://RichString"))
  val mappingTargetBackward = MappingTarget(propertyUri = Uri(PROP_URI), valueType = UriValueType, isBackwardProperty = true)

  it should "serialize and deserialize to/from XML" in {
    implicit val readContext = ReadContext()
    val mappingTargetRoundTrip = XmlSerialization.fromXml[MappingTarget](XmlSerialization.toXml(mappingTarget))
    val mappingTargetCustomRoundTrip = XmlSerialization.fromXml[MappingTarget](XmlSerialization.toXml(mappingTargetCustom))
    val mappingTargetBackwardRoundTrip = XmlSerialization.fromXml[MappingTarget](XmlSerialization.toXml(mappingTargetBackward))
    mappingTarget mustBe mappingTargetRoundTrip
    mappingTargetCustom mustBe mappingTargetCustomRoundTrip
    mappingTargetBackwardRoundTrip mustBe mappingTargetBackward
  }
}
