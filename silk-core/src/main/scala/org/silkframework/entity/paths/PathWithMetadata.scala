package org.silkframework.entity.paths

import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.PathWithMetadata.requiredMetadataKeys
import org.silkframework.entity.{PlainValueTypeSerialization, ValueType}
import org.silkframework.runtime.serialization.WriteContext

import scala.language.implicitConversions
import scala.util.Try

/**
  * A [[Path]] with an expected type statement and a metadata map, containing a minimum of metadata
  * (i.e. the fields original name, type and whether or not is is an XML attribute).
  * This class should be used instead of [[TypedPath]] whenever it is important to keep the origin name
  * or type of a column or when additional metadata needs to be transported.
  * DataIntegration uses this class to transport all necessary metadata of a FieldMetadata object.
  *
  * @param operators the path operators
  * @param valueType the type that has to be considered during processing.
  * @param metadata an immutable map that stores metadata object
  */
class PathWithMetadata (
  operators: List[PathOperator],
  valueType: ValueType,
  val metadata: Map[String, Any]
) extends TypedPath(operators, valueType, PathWithMetadata.isXmlAttribute(metadata)){

  assert(requiredMetadataKeys.flatMap(metadata.get).size == requiredMetadataKeys.size,
    "A PathWithMetadata con only be initialized with the following metadata: " + requiredMetadataKeys.mkString(", "))

  /**
    * Returns the original input String
    */
  def getOriginalName: String = metadata(PathWithMetadata.META_FIELD_ORIGIN_NAME).toString

}

object PathWithMetadata{

  val META_FIELD_XML_ATTRIBUTE: String = "isXmlAttribute"
  val META_FIELD_ORIGIN_NAME: String = "originalName"
  val META_FIELD_VALUE_TYPE: String = "valueType"

  /**
    * converts TypedPath  to PathWIthMetadata
    * ATTENTION: since this will not guarantee the origin column name, it should be used with care
    * @param tp - the TypedPath to convert
    */
  def fromTypedPath(tp: TypedPath): PathWithMetadata = tp match {
    case pwm: PathWithMetadata => pwm
    case tp: TypedPath => apply(tp.operators, tp.valueType, Map[String, Any](
        META_FIELD_XML_ATTRIBUTE -> tp.isAttribute,
        META_FIELD_ORIGIN_NAME -> tp.normalizedSerialization,
        META_FIELD_VALUE_TYPE -> PlainValueTypeSerialization.write(tp.valueType)(vtwc)
      ))
  }

  private val requiredMetadataKeys = Seq(META_FIELD_XML_ATTRIBUTE, META_FIELD_ORIGIN_NAME, META_FIELD_VALUE_TYPE)
  private val vtwc: WriteContext[String] = WriteContext[String]()
  /**
    * the default apply method
    */
  def apply(operators: List[PathOperator], valueType: ValueType, metadata: Map[String, Any]): PathWithMetadata = {
    new PathWithMetadata(operators, valueType, metadata)
  }

  /**
    * @param path - the unparsed, untyped path
    * @param valueType - the ValueType
    * @param isAttribute - indicates whether this is an XML attribute
    */
  def apply(path: String, valueType: ValueType, isAttribute: Boolean)(implicit prefixes: Prefixes): PathWithMetadata = {
    val metadata = Map(
      META_FIELD_XML_ATTRIBUTE -> isAttribute,
      META_FIELD_ORIGIN_NAME -> path.trim,
      META_FIELD_VALUE_TYPE -> PlainValueTypeSerialization.write(valueType)(vtwc)
    )
    apply(UntypedPath.saveApply(path)(prefixes).operators, valueType, metadata)
  }

  /**
    * @param path - the unparsed, untyped path
    * @param valueType - the ValueType
    * @param metadata - an immutable map that stores metadata objects
    */
  def apply(path: String, valueType: ValueType, metadata: Map[String, Any])(implicit prefixes: Prefixes = Prefixes.empty): PathWithMetadata = {
    val m = Map(
      META_FIELD_XML_ATTRIBUTE -> metadata.getOrElse(META_FIELD_XML_ATTRIBUTE, false),
      META_FIELD_ORIGIN_NAME -> metadata.getOrElse(META_FIELD_ORIGIN_NAME, path.trim),
      META_FIELD_VALUE_TYPE -> metadata.getOrElse(META_FIELD_VALUE_TYPE, PlainValueTypeSerialization.write(valueType)(vtwc))
    )
    val oldM = metadata.filterKeys(k => ! requiredMetadataKeys.contains(k))
    apply(UntypedPath.saveApply(path)(prefixes).operators, valueType, m ++ oldM)
  }

  /**
    * determines whether or not the given PathWithMetadata is an XML attribute, based on the provided metadata.
    * @param path - the PathWithMetadata
    */
  def isXmlAttribute(path: PathWithMetadata): Boolean = isXmlAttribute(path.metadata)

  private def isXmlAttribute(metadata: Map[String, Any]): Boolean = metadata.get(META_FIELD_XML_ATTRIBUTE).exists(x =>
    Try(x.asInstanceOf[Boolean]).getOrElse(throw new IllegalArgumentException(META_FIELD_XML_ATTRIBUTE + " needs a boolean value")))
}
