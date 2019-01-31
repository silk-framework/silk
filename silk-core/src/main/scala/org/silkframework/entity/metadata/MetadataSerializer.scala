package org.silkframework.entity.metadata

/**
  * Mixin for metadata SerializationFormats in the context of EntityMetadata
  */
trait MetadataSerializer {

  /**
    * The identifier used to define metadata objects in the map of [[org.silkframework.entity.metadata.EntityMetadata]]
    * NOTE: This method has to be implemented as def and not as val, else the serialization format registration will fail !!!!!!!!!
    */
  def metadataId: String

  /**
    * An indicator whether the LazyMetadata object produced with this serializer will be replaceable (overridable in the Metadata map)
    */
  def replaceableMetadata: Boolean
}

object MetadataSerializer {

  case class SerializerNotFoundException(msg: String = "") extends Exception(msg)

}