package org.silkframework.runtime.serialization

import scala.xml.Node

case class TestXmlStreamEntity(id: String)

object TestXmlStreamEntity {
  implicit object TestXmlStreamEntityFormat extends XmlFormat[TestXmlStreamEntity] {
    override def read(value: Node)(implicit readContext: ReadContext): TestXmlStreamEntity = {
      val id = (value \ "@id").text
      TestXmlStreamEntity(id)
    }

    override def write(value: TestXmlStreamEntity)(implicit writeContext: WriteContext[Node]): Node = {
      <Entity id={value.id}></Entity>
    }
  }
}