package org.silkframework.runtime.serialization

import java.io.OutputStream
import javax.xml.stream.{XMLStreamConstants, XMLStreamReader}
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.stax.StAXSource

import org.silkframework.util.XMLUtils.toXMLUtils

import scala.collection.mutable.ArrayBuffer

/** Test classes for XML streaming format */
case class TestXmlStreamEntities(entities: Seq[TestXmlStreamEntity])

object TestXmlStreamEntities {
  implicit object TestXmlStreamEntitiesStreamingFormat extends StreamXmlFormat[TestXmlStreamEntities] {
    override def read(streamReader: XMLStreamReader)(implicit readContext: ReadContext): TestXmlStreamEntities = {

      val tf = TransformerFactory.newInstance()
      val t = tf.newTransformer
      val entityBuffer = ArrayBuffer[TestXmlStreamEntity]()
      while(streamReader.nextTag() == XMLStreamConstants.START_ELEMENT) {
        val result = new DOMResult()
        println("ENTITY")
        t.transform(new StAXSource(streamReader), result)
        val node = asXml(result.getNode)
        val entity = XmlSerialization.fromXml[TestXmlStreamEntity](node)
        entityBuffer.append(entity)
      }
      TestXmlStreamEntities(entityBuffer)
    }

    override def write(value: TestXmlStreamEntities, outputStream: OutputStream): Unit = {
      for(c <- "<Entities>") {
        outputStream.write(c)
      }
      for(entity <- value.entities) {
        XmlSerialization.toXml[TestXmlStreamEntity](entity).write(outputStream)
      }
      for(c <- "</Entities>") {
        outputStream.write(c)
      }
    }
  }
}