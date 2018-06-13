package org.silkframework.entity

import org.silkframework.dataset.TypedProperty
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}

import scala.xml.Node

/**
  * Constitutes a path with type information.
  *
  * @param ops the path operators
  * @param valueType the type that has to be considered during processing.
  */
case class TypedPath(ops: List[PathOperator], valueType: ValueType, isAttribute: Boolean) extends Path(ops) {

  def this(path: Path, valueType: ValueType, isAttribute: Boolean) = this(path.operators, valueType, isAttribute)

  def property: Option[TypedProperty] = operators match {
    case ForwardOperator(prop) :: Nil   => Some(TypedProperty(prop.uri, valueType, isBackwardProperty = false, isAttribute = isAttribute))
    case BackwardOperator(prop) :: Nil  => Some(TypedProperty(prop.uri, valueType, isBackwardProperty = true, isAttribute = isAttribute))
    case _ => None
  }
}

object TypedPath {

  /**
    *
    * @param path
    * @param valueType
    * @param isAttribute
    * @return
    */
  def apply(path: Path, valueType: ValueType, isAttribute: Boolean): TypedPath = new TypedPath(path.operators, valueType, isAttribute)

  implicit object TypedPathFormat extends XmlFormat[TypedPath] {
    /**
      * Deserializes a value.
      */
    override def read(node: Node)(implicit readContext: ReadContext): TypedPath = {
      val pathNode = (node \ "Path").headOption
      val valueTypeNode = (node \ "ValueType").headOption
      val isAttribute = (node \ "@isAttribute").headOption.exists(_.text == "true")
      (pathNode, valueTypeNode) match {
        case (Some(p), Some(vt)) =>
          val path = Path.parse(p.text.trim)(readContext.prefixes)
          val valueType = XmlSerialization.fromXml[ValueType](vt)
          TypedPath(path, valueType, isAttribute)
        case _ =>
          throw new RuntimeException("TypedPath needs both a Path and ValueType element.")
      }
    }

    /**
      * Serializes a value.
      */
    override def write(typedPath: TypedPath)(implicit writeContext: WriteContext[Node]): Node = {
      implicit val p = writeContext.prefixes
      <TypedPath isAttribute={typedPath.isAttribute.toString} >
        <Path>
          {typedPath.normalizedSerialization}
        </Path>{XmlSerialization.toXml(typedPath.valueType)}
      </TypedPath>
    }
  }

}