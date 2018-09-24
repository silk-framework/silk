package org.silkframework.runtime.serialization

import java.io.{File, FileInputStream, FileOutputStream}

import org.scalatest.{FlatSpec, MustMatchers}

class StreamXmlFormatTest extends FlatSpec with MustMatchers {
  behavior of "Stream XML Format"

  it should "write and read to/from XML in streaming mode" in {
    val entities = TestXmlStreamEntities(sourceEntities = Seq(
        TestXmlStreamEntity("1"),
        TestXmlStreamEntity("2"),
        TestXmlStreamEntity("3")
      ),
      targetEntities = Seq(
        TestXmlStreamEntity("4"),
        TestXmlStreamEntity("5"),
        TestXmlStreamEntity("6")
      )
    )
    testRoundTrip(entities)
    testRoundTrip(entities.copy(targetEntities = Seq()))
    testRoundTrip(entities.copy(sourceEntities = Seq()))
    testRoundTrip(entities.copy(sourceEntities = Seq(), targetEntities = Seq()))
  }

  private def testRoundTrip(entities: TestXmlStreamEntities): Unit = {
    // Check serialization of item works
    implicit val readContext: ReadContext = ReadContext()
    val tempFile = File.createTempFile("xmlSerializationTest", ".xml")
    tempFile.deleteOnExit()
    val outputStream = new FileOutputStream(tempFile)
    StreamXml.write(entities, outputStream)
    outputStream.flush()
    outputStream.close()
    val inputStream = new FileInputStream(tempFile)
    val roundTripEntities = StreamXml.read[TestXmlStreamEntities](inputStream)
    roundTripEntities mustBe entities
  }
}

