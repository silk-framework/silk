package org.silkframework.entity.paths

import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.PathWithMetadata.requiredMetadataKeys
import org.silkframework.entity.{PlainValueTypeSerialization, ValueType}
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
class PathWithMetadata (
  operators: List[PathOperator],
  valueType: ValueType,
  val metadata: Map[String, Any]
) extends TypedPath(operators, valueType, PathWithMetadata.isXmlAttribute(metadata)){

  assert(requiredMetadataKeys.flatMap(metadata.get).size == requiredMetadataKeys.size, "A PathWithMetadata con only be initialized with the following metadata: " + requiredMetadataKeys.mkString(", "))

  /**
    * Returns the original input String
    * @return
    */
  def getOriginalName: Option[String] = metadata.get(PathWithMetadata.META_FIELD_ORIGIN_NAME).map(_.toString)

  def putMetadata(kVs: (String, Any)*): PathWithMetadata = {
    val keys = kVs.map(_._1).distinct
    PathWithMetadata(this.operators, this.valueType, metadata = this.metadata.filterNot(x => keys.contains(x._1)) ++ kVs)
  }
}

object PathWithMetadata{

  val META_FIELD_XML_ATTRIBUTE: String = "isXmlAttribute"
  val META_FIELD_ORIGIN_NAME: String = "originalName"
  val META_FIELD_VALUE_TYPE = "valueType"

  implicit def fromTypedPath(tp: TypedPath): PathWithMetadata = tp match {
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

  def isXmlAttribute(path: PathWithMetadata): Boolean = isXmlAttribute(path.metadata)

  private def isXmlAttribute(metadata: Map[String, Any]): Boolean = metadata.get(META_FIELD_XML_ATTRIBUTE).exists(x =>
    Try(x.asInstanceOf[Boolean]).getOrElse(throw new IllegalArgumentException(META_FIELD_XML_ATTRIBUTE + " needs a boolean value")))
}
