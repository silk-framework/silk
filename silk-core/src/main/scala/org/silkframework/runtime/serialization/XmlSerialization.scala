package org.silkframework.runtime.serialization

import org.silkframework.runtime.plugin.{ParameterStringValue, ParameterTemplateValue, ParameterValues}

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

  def serializeParameters(parameters: Map[String, String]): NodeSeq = {
    NodeSeq.fromSeq(parameters.toSeq.map {
      case (name, v) =>
        <Param name={name} xml:space="preserve">{PCData(v)}</Param>
    })
  }

  def deserializeParameters(node: Node): ParameterValues = {
    val values =
      (node \ "Param").map { p =>
        val name = (p \ "@name").text
        val valueAttr = p \ "@value"
        val value = if(valueAttr.isEmpty) {
          p.text
        } else {
          valueAttr.text
        }
        (name, ParameterStringValue(value))
      }
    ParameterValues(values.toMap)
  }

  def serializeTemplates(templates: Map[String, String]): NodeSeq = {
    NodeSeq.fromSeq(templates.toSeq.map {
      case (name, v) =>
        <Template name={name} xml:space="preserve">{PCData(v)}</Template>
    })
  }

  def deserializeTemplates(node: Node): ParameterValues = {
    val values =
      (node \ "Template").map { p =>
        ((p \ "@name").text, ParameterTemplateValue(p.text))
      }
    ParameterValues(values.toMap)
  }

  def serializeParameterValues(values: ParameterValues): NodeSeq = {
    serializeParameters(values.toStringMap) ++ serializeTemplates(values.templates)
  }

  def deserializeParameterValues(node: Node): ParameterValues = {
    deserializeParameters(node) merge deserializeTemplates(node)
  }
}
