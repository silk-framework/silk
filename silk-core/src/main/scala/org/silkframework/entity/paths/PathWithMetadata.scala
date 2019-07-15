package org.silkframework.entity.paths

import org.silkframework.config.Prefixes
import org.silkframework.entity.{PlainValueTypeSerialization, ValueType}
import org.silkframework.entity.paths.TypedPath._
import org.silkframework.runtime.serialization.WriteContext

import scala.language.implicitConversions
import scala.util.Try

/**
  * A [[Path]] with an expected type statement. It describes the sequence of properties necessary to arrive at the
  * destination object and supplies an expected type of the object found at the destination (and optional additional metadata).
  *
  * @param operators the path operators
  * @param valueType the type that has to be considered during processing.
  * @param metadata an immutable map that stores metadata object
  */
class PathWithMetadata(
  operators: List[PathOperator],
  valueType: ValueType,
  val metadata: Map[String, Any]
) extends TypedPath(operators, valueType, PathWithMetadata.isXmlAttribute(metadata)){

  /**
    * Returns the original input String
    * @return
    */
  def getOriginalName: Option[String] = metadata.get(TypedPath.META_FIELD_ORIGIN_NAME).map(_.toString)


}

object PathWithMetadata{

  implicit def fromTypedPath(tp: TypedPath): PathWithMetadata = tp match {
    case pwm: PathWithMetadata => pwm
    case tp: TypedPath => apply(tp.operators, tp.valueType, Map[String, Any](
        META_FIELD_XML_ATTRIBUTE -> tp.isAttribute,
        META_FIELD_ORIGIN_NAME -> tp.normalizedSerialization,
        META_FIELD_VALUE_TYPE -> PlainValueTypeSerialization.write(tp.valueType)(vtwc)
      ))
  }

  private val vtwc: WriteContext[String] = WriteContext[String]()
  /**
    * the default apply method
    */
  def apply(operators: List[PathOperator], valueType: ValueType, metadata: Map[String, Any]): PathWithMetadata = new PathWithMetadata(operators, valueType, metadata)

  /**
    * @param path - the unparsed, untyped path
    * @param valueType - the ValueType
    * @param isAttribute - indicates whether this is an XML attribute
    */
  def apply(path: String, valueType: ValueType, isAttribute: Boolean = false)(implicit prefixes: Prefixes = Prefixes.empty): PathWithMetadata = {
    val metadata = Map(
      META_FIELD_XML_ATTRIBUTE -> isAttribute,
      META_FIELD_ORIGIN_NAME -> path,
      META_FIELD_VALUE_TYPE -> PlainValueTypeSerialization.write(valueType)(vtwc)
    )
    PathWithMetadata.apply(UntypedPath.saveApply(path)(prefixes).operators, valueType, metadata)
  }

  /**
    * @param path - the untyped path
    * @param valueType - the ValueType
    * @param metadata - an immutable map that stores metadata objects
    */
  def apply(path: Path, valueType: ValueType, metadata: Map[String, Any]): PathWithMetadata = apply(path.operators, valueType, metadata)

  /**
    * @param ops - the path operators
    * @param valueType - the ValueType
    * @param isAttribute - indicates whether this is an XML attribute
    */
  def apply(ops: List[PathOperator], valueType: ValueType, isAttribute: Boolean): PathWithMetadata =
    apply(ops, valueType, if(isAttribute) Map(META_FIELD_XML_ATTRIBUTE -> true) else Map.empty[String, Any]) //if not an attribute, we can leave map empty, false is assumed


  def isXmlAttribute(path: PathWithMetadata): Boolean = isXmlAttribute(path.metadata)

  private def isXmlAttribute(metadata: Map[String, Any]): Boolean = metadata.get(TypedPath.META_FIELD_XML_ATTRIBUTE).exists(x =>
    Try(x.asInstanceOf[Boolean]).getOrElse(throw new IllegalArgumentException(TypedPath.META_FIELD_XML_ATTRIBUTE + " needs a boolean value")))
}
