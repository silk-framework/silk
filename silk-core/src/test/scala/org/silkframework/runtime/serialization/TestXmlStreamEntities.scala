package org.silkframework.runtime.serialization

import java.io.OutputStream
import javax.xml.stream.XMLStreamReader
import javax.xml.transform.{Transformer, TransformerFactory}

import org.silkframework.util.XMLUtils.toXMLUtils

/** Test classes for XML streaming format */
case class TestXmlStreamEntities(sourceEntities: Seq[TestXmlStreamEntity], targetEntities: Seq[TestXmlStreamEntity])

object TestXmlStreamEntities {
  implicit object TestXmlStreamEntitiesStreamingFormat extends StreamXmlFormat[TestXmlStreamEntities] {
    final val ROOT = "TestXmlStreamEntities"
    final val ENTITIES = "Entities"
    final val SOURCE_ENTITIES = "SourceEntities"
    final val TARGET_ENTITIES = "TargetEntities"

    override def read(implicit streamReader: XMLStreamReader, readContext: ReadContext): TestXmlStreamEntities = {
      val transformerFactory = TransformerFactory.newInstance()
      implicit val transformer: Transformer = transformerFactory.newTransformer
      placeOnStartTag(SOURCE_ENTITIES)
      streamReader.nextTag() // Place on first entity or end tag
      val sourceEntities = readObjects[TestXmlStreamEntity](expectedTag =  Some("Entity"))
      placeOnStartTag(TARGET_ENTITIES)
      streamReader.nextTag() // Place on first entity or end tag
      val targetEntities = readObjects[TestXmlStreamEntity](expectedTag =  Some("Entity"))
      TestXmlStreamEntities(sourceEntities, targetEntities)
    }

    override def write(value: TestXmlStreamEntities, outputStream: OutputStream): Unit = {
      implicit val os: OutputStream = outputStream
      writeStartTag(ROOT)
        writeStartTag(SOURCE_ENTITIES)
          value.sourceEntities foreach { entity => XmlSerialization.toXml(entity).write(outputStream)}
        writeEndTag(SOURCE_ENTITIES)
        writeStartTag(TARGET_ENTITIES)
          value.targetEntities foreach { entity => XmlSerialization.toXml(entity).write(outputStream)}
        writeEndTag(TARGET_ENTITIES)
      writeEndTag(ROOT)
    }
  }
}