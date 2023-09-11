package org.silkframework.entity


import org.silkframework.runtime.serialization.{ReadContext, TestReadContext, XmlSerialization}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class ValueTypeTest extends AnyFlatSpec with Matchers {
  behavior of "ValueType"

  implicit val readContext: ReadContext = TestReadContext()

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
  for(valueType <- ValueType.allValueType.flatMap(_.toOption)) {
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
      it should "reject invalid values" in {
        for (value <- annotation.invalidValues()) {
          if(valueType.validate(value)) {
            fail(s"$valueType did not reject '$value' as an invalid value.")
          }
        }
      }
    }
  }
}
