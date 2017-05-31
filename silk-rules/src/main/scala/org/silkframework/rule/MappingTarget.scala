package org.silkframework.rule

import org.silkframework.dataset.TypedProperty
import org.silkframework.entity.{AutoDetectValueType, ValueType}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.Uri
import org.silkframework.runtime.serialization.XmlSerialization._
import scala.xml.Node
import scala.language.implicitConversions

case class MappingTarget(propertyUri: Uri, valueType: ValueType = AutoDetectValueType) {

  override def toString: String = {
    if(valueType == AutoDetectValueType)
      propertyUri.toString
    else
      s"$propertyUri (${valueType.label})"
  }

}

object MappingTarget {

  implicit object MappingTargetFormat extends XmlFormat[MappingTarget] {

    /**
      * Deserializes a value.
      */
    override def read(value: Node)(implicit readContext: ReadContext): MappingTarget = {
      val uri = (value \ "@uri").text.trim
      val valueTypeNode = (value \ "ValueType").head
      MappingTarget(Uri.parse(uri, readContext.prefixes), fromXml[ValueType](valueTypeNode))
    }

    /**
      * Serializes a value.
      */
    override def write(value: MappingTarget)(implicit writeContext: WriteContext[Node]): Node = {
      <MappingTarget uri={value.propertyUri.uri}>
        {toXml[ValueType](value.valueType)}
      </MappingTarget>
    }
  }

  implicit def toTypedProperty(mt: MappingTarget): TypedProperty = TypedProperty(mt.propertyUri.uri, mt.valueType)

}
