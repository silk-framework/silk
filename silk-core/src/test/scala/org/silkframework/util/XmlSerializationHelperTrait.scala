package org.silkframework.util

import java.io.{File, FileInputStream, FileOutputStream}

import org.scalatest.MustMatchers
import org.silkframework.runtime.serialization._

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

  def testRoundTripSerializationStreaming[T <: AnyRef](obj: T)
                                                      (implicit format: StreamXmlFormat[T]): Unit = {
    implicit val readContext: ReadContext = ReadContext()
    val tempFile = File.createTempFile("xmlSerializationTest", ".xml")
    tempFile.deleteOnExit()
    val outputStream = new FileOutputStream(tempFile)
    StreamXml.write(obj, outputStream)
    outputStream.flush()
    outputStream.close()
    val inputStream = new FileInputStream(tempFile)
    val roundTripEntities = StreamXml.read[T](inputStream)
    roundTripEntities mustBe obj
  }
}
