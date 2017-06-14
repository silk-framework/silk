package org.silkframework.rule

import org.silkframework.dataset.TypedProperty
import org.silkframework.entity.{AutoDetectValueType, ValueType}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.Uri
import org.silkframework.runtime.serialization.XmlSerialization._
import scala.xml.Node
import scala.language.implicitConversions

case class MappingTarget(propertyUri: Uri, valueType: ValueType = AutoDetectValueType, isBackwardProperty: Boolean = false) {

  override def toString: String = {
    val addedType = if(valueType == AutoDetectValueType) {
      propertyUri.toString
    } else {
      s"$propertyUri (${valueType.label})"
    }
    if(isBackwardProperty) "\\" + addedType else addedType
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
      val valueTypeNode = (value \ "ValueType").head
      MappingTarget(Uri.parse(uri, readContext.prefixes), fromXml[ValueType](valueTypeNode), isBackwardProperty = isBackwardProperty)
    }

    /**
      * Serializes a value.
      */
    override def write(value: MappingTarget)(implicit writeContext: WriteContext[Node]): Node = {
      <MappingTarget uri={value.propertyUri.uri} isBackwardProperty={value.isBackwardProperty.toString}>
        {toXml[ValueType](value.valueType)}
      </MappingTarget>
    }
  }

  implicit def toTypedProperty(mt: MappingTarget): TypedProperty = TypedProperty(mt.propertyUri.uri, mt.valueType, mt.isBackwardProperty)

}
