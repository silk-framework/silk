package org.silkframework.entity

import org.silkframework.dataset.TypedProperty
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.util.Uri

import scala.xml.Node

/**
  * Constitutes a path with type information.
  *
  * @param path      the path
  * @param valueType the type that has to be considered during processing.
  */
case class TypedPath(path: Path, valueType: ValueType) {

  def propertyUri: Option[Uri] = path.propertyUri

  def property: Option[TypedProperty] = path.propertyUri.map(uri => TypedProperty(uri.toString, valueType))
}

object TypedPath {

  implicit object TypedPathFormat extends XmlFormat[TypedPath] {
    /**
      * Deserializes a value.
      */
    override def read(node: Node)(implicit readContext: ReadContext): TypedPath = {
      val pathNode = (node \ "Path").headOption
      val valueTypeNode = (node \ "ValueType").headOption
      (pathNode, valueTypeNode) match {
        case (Some(p), Some(vt)) =>
          val path = Path.parse(p.text.trim)(readContext.prefixes)
          val valueType = XmlSerialization.fromXml[ValueType](vt)
          TypedPath(path, valueType)
        case _ =>
          throw new RuntimeException("TypedPath needs both a Path and ValueType element.")
      }
    }

    /**
      * Serializes a value.
      */
    override def write(typedPath: TypedPath)(implicit writeContext: WriteContext[Node]): Node = {
      implicit val p = writeContext.prefixes
      <TypedPath>
        <Path>
          {typedPath.path.serialize}
        </Path>{XmlSerialization.toXml(typedPath.valueType)}
      </TypedPath>
    }
  }

}