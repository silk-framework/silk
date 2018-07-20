package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization.SerializationFormat

import scala.collection.mutable

trait MetadataSerializerRegistry[Format <: Any] {

  /**
    * Each serialization format needs a dedicated Exception serializer
    */
  val exceptionSerializer: SerializationFormat[Throwable, Format] with MetadataSerializer

  /* Serializer Registry */

  private val SerializerRegistry = new mutable.HashMap[String, SerializationFormat[_, Format] with MetadataSerializer]

  def registerSerializationFormat(key: String, sf: SerializationFormat[_, Format] with MetadataSerializer): Unit ={
    //add entry for each mime type
    SerializerRegistry.put(key, sf)
  }

  def getSerializationFormat[T](key: String): Option[SerializationFormat[T, Format] with MetadataSerializer] ={
    SerializerRegistry.get(key) match{
      case Some(ser) => ser.valueType match{
        case _:T => Some(ser.asInstanceOf[SerializationFormat[T, Format] with MetadataSerializer])              //NOTE: unchecked conversion to T
        case _ => None
      }
      case None => None
    }
  }

  def getSerializer[T](key: String): SerializationFormat[T, Format] with MetadataSerializer = getSerializationFormat[T](key).getOrElse(throwSerializerNotFound(key))

  def throwSerializerNotFound[T](key: String): T =
    throw MetadataSerializer.SerializerNotFoundException("Serializer for category " + key + " was not found in this registry: " + this.getClass.getName)

  def listAllSerializers: List[SerializationFormat[_, Format]] = SerializerRegistry.values.toList.distinct
}
