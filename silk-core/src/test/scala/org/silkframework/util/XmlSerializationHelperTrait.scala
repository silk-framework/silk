package org.silkframework.util

import org.scalatest.MustMatchers
import org.silkframework.runtime.serialization.{ReadContext, XmlFormat, XmlSerialization}

/**
  * Helper methods for testing XML serialoization and deserialization.
  */
trait XmlSerializationHelperTrait extends MustMatchers {
  /**
    * Checks for equality of an object after it has been serialized and deserialized again.
    *
    * @param obj        The object that should be serialized and deserialized again.
    * @param extraTests Allows to define extra tests on the original and roundtrip object.
    */
  def testRoundTripSerialization[T <: AnyRef](obj: T,
                                              extraTests: (T, T) => Unit = (a: T, b: T) => {})
                                             (implicit format: XmlFormat[T]): Unit = {
    implicit val readContext = ReadContext()
    val xmlNode = XmlSerialization.toXml[T](obj)
    val roundTripObjType = XmlSerialization.fromXml[T](xmlNode)
    roundTripObjType mustBe obj
    extraTests(obj, roundTripObjType)
  }
}
