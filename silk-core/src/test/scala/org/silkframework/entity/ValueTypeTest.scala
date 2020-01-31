package org.silkframework.entity

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}

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

  // Test all value types without parameters for now
  for(valueType <- ValueType.allValueType.flatMap(_.right.toOption)) {
    testValueType(valueType)
  }

  def testValueType(valueType: ValueType): Unit = {
    val annotation = valueType.getClass.getAnnotation(classOf[ValueTypeAnnotation])
    if (annotation != null) {
      valueType.id should "accept valid values" in {
        for (value <- annotation.validValues()) {
          if(!valueType.validate(value)) {
            fail(s"$valueType did not accept '$value' as a valid value.")
          }
        }
      }
      it should "reject valid values" in {
        for (value <- annotation.invalidValues()) {
          if(valueType.validate(value)) {
            fail(s"$valueType did not reject '$value' as an invalid value.")
          }
        }
      }
    }
  }
}
