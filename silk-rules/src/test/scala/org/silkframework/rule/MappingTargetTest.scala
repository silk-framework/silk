package org.silkframework.rule

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.{CustomValueType, FloatValueType, UriValueType, ValueType}
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.util.Uri

/**
  * Created on 11/22/16.
  */
class MappingTargetTest extends FlatSpec with MustMatchers {
  behavior of "MatchingTarget"

  private val PROP_URI = "http://uri"
  val mappingTarget = MappingTarget(propertyUri = Uri(PROP_URI), valueType = ValueType.FLOAT)
  val mappingTargetCustom = MappingTarget(propertyUri = Uri(PROP_URI), valueType = CustomValueType("http://RichString"))
  val mappingTargetBackward = MappingTarget(propertyUri = Uri(PROP_URI), valueType = ValueType.URI, isBackwardProperty = true)
  val mappingTargetAttribute = MappingTarget(propertyUri = Uri(PROP_URI), valueType = ValueType.URI, isAttribute = true)

  it should "serialize and deserialize to/from XML" in {
    roundTripTest(mappingTarget)
    roundTripTest(mappingTargetCustom)
    roundTripTest(mappingTargetBackward)
    roundTripTest(mappingTargetAttribute)
  }

  private def roundTripTest(mappingTarget: MappingTarget): Unit = {
    implicit val readContext = ReadContext()
    val mappingTargetRoundTrip = XmlSerialization.fromXml[MappingTarget](XmlSerialization.toXml(mappingTarget))
    mappingTarget mustBe mappingTargetRoundTrip
  }
}
