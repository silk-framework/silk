package org.silkframework.runtime.serialization

import scala.xml.{Node, NodeSeq, PCData}

/**
 * Serializes between classes and XML.
 * In order to be serializable a class needs to provide an implicit XmlFormat object.
 */
object XmlSerialization {

  def toXml[T](value: T)(implicit format: XmlFormat[T], writeContext: WriteContext[Node] = WriteContext[Node](projectId = None)): Node = {
    format.write(value)
  }

  def fromXml[T](node: Node)(implicit format: XmlFormat[T], readContext: ReadContext): T = {
    format.read(node)
  }

  def serializeParameter(parameters: Map[String, String]): NodeSeq = {
    NodeSeq.fromSeq(parameters.toSeq.map {
      case (name, v) =>
        val vPCdata = PCData(v)
        <Param name={name} xml:space="preserve">{vPCdata}</Param>
    })
  }

  def deserializeParameters(node: Node): Map[String, String] = {
    (node \ "Param").map { p =>
      val name = (p \ "@name").text
      val valueAttr = p \ "@value"
      val value = if(valueAttr.isEmpty) {
        p.text
      } else {
        valueAttr.text
      }
      (name, value)
    }.toMap
  }
}
