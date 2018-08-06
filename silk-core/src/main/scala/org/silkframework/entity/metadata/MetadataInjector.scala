package org.silkframework.entity.metadata

import org.silkframework.entity.Entity
import org.silkframework.runtime.serialization.SerializationFormat

/**
  * Describes a transformation of a given Entity into a new metadata object, which is then stored in the [[org.silkframework.entity.Entity.metadata]] object
  */
trait MetadataInjector[Typ, Ser] extends Serializable {

  /**
    * The metadata serializer to be used
    */
  def serializer: SerializationFormat[Typ, Ser] with MetadataSerializer

  /**
    * The identifier used to define metadata objects in the map of [[org.silkframework.entity.metadata.EntityMetadata]]
    * NOTE: Implement this as a def, else the automatic registration of this injector will fail.
    */
  def metadataId: String

  /**
    * Will be executed before the first [[compute]]
    */
  def beforeAll(): Unit = {}

  /**
    * Computes e new LazyMetadata object for the given Entity
    */
  def compute(entity: Entity, obj: Option[Any]): Option[LazyMetadata[Typ, Ser]]

  /**
    * Will be executed after the last [[compute]]
    */
  def afterAll(): Unit = {}

  private def computeAndValidate(entity: Entity, obj: Option[Any]): Option[LazyMetadata[Typ, Ser]] ={
    compute(entity, obj).map(lm =>{
      //test whether the generated metadata object is (ir-)replaceable as defined in the pertaining serializer
      assert(lm.metadata.isEmpty || lm.isReplaceable == serializer.replaceableMetadata, "The generated metadata objects replaceable indicator does not match its serializer." +
        "Make sure when implementing a metadata injector to use a LazyMetadata container with the [[IrreplaceableMetadata]] trait, when setting isReplaceable to false.")
      lm
    })
  }

  private def getEmptyMetadataInstance: EntityMetadata[Ser] = EntityMetadata.empty[Ser](serializer.serializedType.asInstanceOf[Class[Ser]]) match{
    case Some(em) => em
    case None => throw new NotImplementedError("No implementation of [[EntityMetadata]] for serialization type " + serializer.serializedType.getName + " was found.")
  }

  /**
    * Generates new metadata objects for each Entity and stores it under the metadataId in the EntityMetadata container
    */
  def injectMetadata(entity: Entity, obj: Option[Any]): Entity = {
    computeAndValidate(entity, obj) match{
      case Some(lm) => entity.metadata match{
        case empty: EntityMetadata[_] if empty.isEmpty => entity.copy(metadata = getEmptyMetadataInstance.addReplaceMetadata(metadataId, lm))
        case inst: EntityMetadata[Ser] => inst.get(metadataId) match{
          case Some(m) if m.isReplaceable || m.metadata.isEmpty => entity.copy(metadata = inst.addReplaceMetadata(metadataId, lm))
          case None => entity.copy(metadata = inst.addReplaceMetadata(metadataId, lm))
          case _ => entity
        }
        case _ => throw new IllegalStateException("No metadata map found for entity " + entity.uri)
      }
      case None => entity
    }
  }
}
