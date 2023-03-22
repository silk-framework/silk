package org.silkframework.runtime.serialization

import org.silkframework.runtime.plugin.{ParameterStringValue, ParameterTemplateValue, ParameterValue, ParameterValues}

import scala.xml.{Node, NodeSeq, PCData}

/**
 * Serializes between classes and XML.
 * In order to be serializable a class needs to provide an implicit XmlFormat object.
 */
object XmlSerialization {

  def toXml[T](value: T)(implicit format: XmlFormat[T], writeContext: WriteContext[Node] = WriteContext.empty[Node]): Node = {
    format.write(value)
  }

  def fromXml[T](node: Node)(implicit format: XmlFormat[T], readContext: ReadContext): T = {
    format.read(node)
  }

  /**
    * Writes plugin parameter values to a resource.
    */
  def serializeParameters(parameters: ParameterValues): NodeSeq = {
    NodeSeq.fromSeq(parameters.values.map {
      case (key, value) => serializeParameter(key, value)
    }.toSeq)
  }

  /**
    * Reads plugin parameter values from a resource.
    */
  def deserializeParameters(node: Node): ParameterValues = {
    val values = {
      for {
        child <- node.child
        value <- deserializeParameter(child)
      } yield {
        val name = (child \ "@name").text
        (name, value)
      }
    }
    ParameterValues(values.toMap)
  }

  private def serializeParameter(key: String, value: ParameterValue): Node = {
    value match {
      case ParameterStringValue(v) =>
        <Param name={key} xml:space="preserve">{PCData(v)}</Param>
      case ParameterTemplateValue(template) =>
        <Template name={key} xml:space="preserve">{PCData(template)}</Template>
      case values: ParameterValues =>
        <Params name={key}>{serializeParameters(values)}</Params>
      case _ =>
        throw new IllegalArgumentException("Unsupported parameter type: " + value.getClass)
    }
  }

  private def deserializeParameter(node: Node): Option[ParameterValue] = {
    node.label match {
      case "Param" =>
        val valueAttr = node \ "@value"
        if (valueAttr.nonEmpty) {
          Some(ParameterStringValue(valueAttr.text))
        } else {
          Some(ParameterStringValue(node.text))
        }
      case "Template" =>
        Some(ParameterTemplateValue(node.text))
      case "Params" =>
        Some(deserializeParameters(node))
      case _ =>
        None
    }
  }
}
