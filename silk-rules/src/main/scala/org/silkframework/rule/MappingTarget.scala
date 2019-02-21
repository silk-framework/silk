package org.silkframework.rule

import org.silkframework.dataset.TypedProperty
import org.silkframework.entity._
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.Uri

import scala.language.implicitConversions
import scala.xml.Node

/**
  * The target of a mapping.
  *
  * @param propertyUri The URI of the property.
  * @param valueType The target type against which all values are validated.
  * @param isBackwardProperty In data sinks that with graph-like data models, a relation from the target to the source is generated.
  * @param isAttribute In data sinks that support attributes, such as XML, an attribute is generated.
  */
case class MappingTarget(propertyUri: Uri,
                         valueType: ValueType = StringValueType,
                         isBackwardProperty: Boolean = false,
                         isAttribute: Boolean = false) {

  override def toString: String = {
    val sb = new StringBuilder(propertyUri.uri)
    if(valueType != UntypedValueType) {
      sb += ' '
      sb ++= valueType.label
    }
    if(isBackwardProperty) {
      sb.insert(0, '\\')
    }
    if(isAttribute) {
      sb ++= " (Attribute)"
    }
    sb.toString()
  }

  /** Representation of the mapping target as Silk Path */
  def asPath(): Path = {
    val op = if (isBackwardProperty) BackwardOperator(propertyUri.uri) else ForwardOperator(propertyUri.uri)
    Path(List(op))
  }

  def asTypedPath(): TypedPath = {
    TypedPath(asPath(), valueType, isAttribute)
  }
}

object MappingTarget {

  implicit object MappingTargetFormat extends XmlFormat[MappingTarget] {

    import org.silkframework.runtime.serialization.XmlSerialization._

    /**
      * Deserializes a value.
      */
    override def read(value: Node)(implicit readContext: ReadContext): MappingTarget = {
      val uri = (value \ "@uri").text.trim
      val isBackwardProperty = (value \ "@isBackwardProperty").headOption.exists(_.text == "true")
      val isAttribute = (value \ "@isAttribute").headOption.exists(_.text == "true")
      val valueTypeNode = (value \ "ValueType").head
      MappingTarget(Uri(uri), fromXml[ValueType](valueTypeNode), isBackwardProperty, isAttribute)
    }

    /**
      * Serializes a value.
      */
    override def write(value: MappingTarget)(implicit writeContext: WriteContext[Node]): Node = {
      <MappingTarget uri={value.propertyUri.uri} isBackwardProperty={value.isBackwardProperty.toString} isAttribute={value.isAttribute.toString}>
        {toXml[ValueType](value.valueType)}
      </MappingTarget>
    }
  }

  implicit def toTypedProperty(mt: MappingTarget): TypedProperty = TypedProperty(mt.propertyUri.uri, mt.valueType, mt.isBackwardProperty, mt.isAttribute)

}
