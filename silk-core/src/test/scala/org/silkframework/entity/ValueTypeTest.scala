package org.silkframework.entity

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}

/**
  * Created on 11/22/16.
  */
class ValueTypeTest extends FlatSpec with MustMatchers {
  behavior of "ValueType"

  implicit val readContext = ReadContext()

  it should "serialize and deserialize object value types to/from XML" in {
    val objTypes = Seq(ValueType.FLOAT, ValueType.DOUBLE, ValueType.URI, ValueType.UNTYPED, ValueType.DATE_TIME)

    val xmlNodes = objTypes.map(XmlSerialization.toXml[ValueType])
    val roundTripObjTypes = xmlNodes.map(XmlSerialization.fromXml[ValueType])
    roundTripObjTypes mustBe objTypes
  }

  it should "serialize and deserialize class value types to/from XML" in {
    val objTypes = Seq(CustomValueType("http://uri"), LanguageValueType("en"))

    val xmlNodes = objTypes.map(XmlSerialization.toXml[ValueType])
    val roundTripObjTypes = xmlNodes.map(XmlSerialization.fromXml[ValueType])
    roundTripObjTypes mustBe objTypes
  }
}
