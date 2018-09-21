package org.silkframework.runtime.serialization

import java.io.{File, FileInputStream, FileOutputStream}

import org.scalatest.{FlatSpec, MustMatchers}

class StreamXmlFormatTest extends FlatSpec with MustMatchers {
  behavior of "Stream XML Format"

  it should "write and read to/from XML in streaming mode" in {
    val entities = TestXmlStreamEntities(Seq(
      TestXmlStreamEntity("1"),
      TestXmlStreamEntity("2"),
      TestXmlStreamEntity("3")
    ))
    // Check serialization of item works
    implicit val readContext: ReadContext = ReadContext()
    XmlSerialization.fromXml[TestXmlStreamEntity](XmlSerialization.toXml(entities.entities.head)) mustBe entities.entities.head
    val tempFile = File.createTempFile("xmlSerializationTest", ".xml")
    tempFile.deleteOnExit()
    val outputStream = new FileOutputStream(tempFile)
    StreamXml.write(entities, outputStream)
    outputStream.flush()
    outputStream.close()
    val inputStream = new FileInputStream(tempFile)
    val roundTripEntities = StreamXml.read[TestXmlStreamEntities](inputStream)
    entities mustBe roundTripEntities
  }
}

