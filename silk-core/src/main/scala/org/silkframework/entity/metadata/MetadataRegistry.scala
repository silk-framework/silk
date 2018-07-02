package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization.SerializationFormat

import scala.collection.mutable

trait MetadataRegistry[Format <: Any] {

  /**
    * Each serialization format needs a dedicated Exception serializer
    */
  val exceptionSerializer: SerializationFormat[Throwable, Format]

  /* Serializer Registry */

  private val SerializerRegistry = new mutable.HashMap[String, SerializationFormat[_, Format]]

  def registerSerializationFormat(key: String, sf: SerializationFormat[_, Format]): Unit ={
    //add entry for each mime type
    SerializerRegistry.put(key, sf)
  }

  def getSerializationFormat[T](key: String): Option[SerializationFormat[T, Format]] ={
    SerializerRegistry.get(key) match{
      case Some(ser) => ser.valueType match{
        case _:T => Some(ser.asInstanceOf[SerializationFormat[T, Format]])
        case _ => None
      }
      case None => None
    }
  }

  def listAllSerializers: List[SerializationFormat[_, Format]] = SerializerRegistry.values.toList.distinct
}
