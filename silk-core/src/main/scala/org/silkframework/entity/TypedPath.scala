package org.silkframework.entity

import org.silkframework.config.Prefixes
import org.silkframework.dataset.TypedProperty
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}

import scala.util.Try
import scala.xml.Node

/**
  * Constitutes a path with type information.
  *
  * @param ops the path operators
  * @param valueType the type that has to be considered during processing.
  * @param metadata an immutable map that stores metadata object
  */
case class TypedPath(
    private val ops: List[PathOperator],
    valueType: ValueType,
    metadata: Map[String, Any]
  ) extends Path(ops) {

  /**
    * checks metadata for an positive entry for the IS_ATTRIBUTE_KEY key
    * earmarks XML attributes
    */
  def isAttribute: Boolean = metadata.get(TypedPath.META_FIELD_XML_ATTRIBUTE).exists(x =>
    Try(x.asInstanceOf[Boolean]).getOrElse(throw new IllegalArgumentException(TypedPath.META_FIELD_XML_ATTRIBUTE + " needs a boolean value")))

  /**
    * Returns the original input String
    * @return
    */
  def getOriginalName: Option[String] = metadata.get(TypedPath.META_FIELD_ORIGIN_NAME).map(_.toString)

  def toSimplePath: Path = Path(operators)

  /**
    * Returns a typed property if this is a path of length one.
    * Returns None otherwise.
    */
  def property: Option[TypedProperty] = operators match {
    case ForwardOperator(prop) :: Nil   => Some(TypedProperty(prop.uri, valueType, isBackwardProperty = false, isAttribute = isAttribute))
    case BackwardOperator(prop) :: Nil  => Some(TypedProperty(prop.uri, valueType, isBackwardProperty = true, isAttribute = isAttribute))
    case _ => None
  }

  /**
    * Additional equals ignoring the ValueType if one of the paths feature an UntypedValueType
    * @param tp - the comparison object
    */
  def equalsUntyped(tp: TypedPath): Boolean = {//TODO TypedPath change: new
    tp match {
      case tp@TypedPath(_, otherValueType, _) =>
        // if one of the comparison objects are untyped, we ignore the type all together
        valueType.equalsOrIndifferentTo(otherValueType) &&
          tp.normalizedSerialization == normalizedSerialization &&
          tp.isAttribute == isAttribute
      case _ =>
        false
    }
  }

  override def equals(other: Any): Boolean = {//TODO TypedPath change: new
    other match {
      case tp@TypedPath(_, otherValueType, _) =>
        valueType == otherValueType &&
        tp.normalizedSerialization == normalizedSerialization &&
        tp.isAttribute == isAttribute
      case _ =>
        false
    }
  }
}

object TypedPath {

  val META_FIELD_XML_ATTRIBUTE: String = "isXmlAttribute"
  val META_FIELD_ORIGIN_NAME: String = "originalName"

  /**
    * @param path - the untyped path
    * @param valueType - the ValueType
    * @param isAttribute - indicates whether this is an XML attribute
    */
  def apply(path: Path, valueType: ValueType, isAttribute: Boolean): TypedPath = apply(path.operators, valueType, isAttribute)


  /**
    * @param path - the untyped path
    * @param valueType - the ValueType
    * @param metadata - an immutable map that stores metadata objects
    */
  def apply(path: Path, valueType: ValueType,  metadata: Map[String, Any]): TypedPath = apply(path.operators, valueType, metadata)

  /**
    * @param path - the unparsed, untyped path
    * @param valueType - the ValueType
    * @param isAttribute - indicates whether this is an XML attribute
    */
  def apply(path: String, valueType: ValueType, isAttribute: Boolean = false)(implicit prefixes: Prefixes = Prefixes.empty): TypedPath = {
    val metadata = Map(
      META_FIELD_XML_ATTRIBUTE -> isAttribute,
      META_FIELD_ORIGIN_NAME -> path
    )
    apply(Path.saveApply(path)(prefixes).operators, valueType, metadata)
  }

  /**
    * @param ops - the path operators
    * @param valueType - the ValueType
    * @param isAttribute - indicates whether this is an XML attribute
    */
  def apply(ops: List[PathOperator], valueType: ValueType, isAttribute: Boolean): TypedPath =
    apply(ops, valueType, if(isAttribute) Map(META_FIELD_XML_ATTRIBUTE -> true) else Map.empty[String, Any]) //if not an attribute, we can leave map empty, false is assumed

  /**
    * Empty TypedPath (used as filler or duds)
    * @return
    */
  def empty: TypedPath = TypedPath(Path.empty, UntypedValueType, isAttribute = false)//TODO TypedPath change:

  /**
    * Will remove a given subpath prefix from the operator list of a TypedPath
    * @param path - the path to be reduced
    * @param subPath -  the sub path
    */
  def reducePath(path: Path, subPath: Path): Path = if(path.operators.startsWith(subPath.operators)){
    path match{
      case tp: TypedPath => TypedPath(path.operators.drop(subPath.operators.size), tp.valueType, tp.isAttribute)
      case _ => Path(path.operators.drop(subPath.operators.size))
    }
  }
  else{
    path
  }

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
          val valueType = XmlSerialization.fromXml[ValueType](vt)
          TypedPath(p.text.trim, valueType, isAttribute)(readContext.prefixes)
        case _ =>
          throw new RuntimeException("TypedPath needs both a Path and ValueType element.")
      }
    }

    /**
      * Serializes a value.
      */
    override def write(typedPath: TypedPath)(implicit writeContext: WriteContext[Node]): Node = {
      implicit val p: Prefixes = writeContext.prefixes
      <TypedPath isAttribute={typedPath.isAttribute.toString} >
        <Path>{typedPath.normalizedSerialization}</Path>
        {XmlSerialization.toXml(typedPath.valueType)}
      </TypedPath>
    }
  }

}
