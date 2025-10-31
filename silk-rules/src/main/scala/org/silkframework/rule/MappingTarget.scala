package org.silkframework.rule

import org.silkframework.dataset.TypedProperty
import org.silkframework.entity._
import org.silkframework.entity.paths.{BackwardOperator, DirectionalPathOperator, ForwardOperator, TypedPath, UntypedPath}
import org.silkframework.rule.execution.local.MultipleValuesException
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import scala.language.implicitConversions
import scala.xml.Node

/**
  * The target of a mapping.
  *
  * @param propertyUri The URI of the property.
  * @param valueType The target type against which all values are validated.
  * @param isBackwardProperty In data sinks that with graph-like data models, a relation from the target to the source is generated.
  * @param isAttribute If true, a single value is expected and supporting datasets will not use arrays etc.
  *                    In XML, attributes will be used instead of nested elements.
  */
case class MappingTarget(propertyUri: Uri,
                         valueType: ValueType = ValueType.STRING,
                         isBackwardProperty: Boolean = false,
                         isAttribute: Boolean = false) {

  /**
    * Asserts that the given values are valid for this target.
    *
    * @throws ValidationException If the provided values are not valid.
    */
  def validate(values: Seq[String]): Unit = {
    // value type
    for {
      value <- values
      if !valueType.validate(value)
    } {
      throw new ValidationException(s"Value '$value' is not a valid ${valueType.label}")
    }
    // cardinality
    if(isAttribute && values.size > 1) {
      throw new MultipleValuesException(s"Property '$propertyUri' is only allowed to have one value per entity, but instead got ${values.size} values.")
    }
  }

  override def toString: String = {
    val sb = new StringBuilder(propertyUri.uri)
    if(valueType != ValueType.UNTYPED) {
      sb += ' '
      sb ++= valueType.label
    }
    if(isBackwardProperty) {
      sb.insert(0, '\\')
    }
    if(isAttribute) {
      sb ++= " (single value)"
    }
    sb.toString()
  }

  def asPathOperator(): DirectionalPathOperator = {
    if (isBackwardProperty) BackwardOperator(propertyUri.uri) else ForwardOperator(propertyUri.uri)
  }

  /** Representation of the mapping target as Silk Path */
  def asPath(): UntypedPath = {
    UntypedPath(List(asPathOperator()))
  }

  def asTypedPath(): TypedPath = {
    TypedPath(asPath(), valueType, isAttribute)
  }
}

object MappingTarget {

  /**
   * Creates a MappingTarget for an object property.
   */
  def obj(propertyUri: Uri,
          isBackwardProperty: Boolean = false,
          isAttribute: Boolean = false): MappingTarget = {
    MappingTarget(propertyUri, ValueType.URI, isBackwardProperty, isAttribute)
  }

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
