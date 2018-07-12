package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization._

import scala.collection.mutable

/**
  * Entity metadata container, implemented by an immutable Map[String, LazyMetadata]
  * @tparam Serialization - the serialization format each metadata object can be serialized to
  */
trait EntityMetadata[Serialization] extends Map[String, LazyMetadata[_, Serialization]] with Serializable{

  implicit val serTag: Class[Serialization]

  /**
    * get metadata object
    * @param key - with this key
    * @tparam Typ - of this Type
    * @return - the metadata object or None
    */
  def getLazyMetadata[Typ](key: String)(implicit tt: Class[Typ]): LazyMetadata[Typ, Serialization] = metadata.get(key) match{
    case Some(lm: LazyMetadata[_, _]) => lm.metadata match{
      case Some(x: Any) if tt.isAssignableFrom(x.getClass) => lm.asInstanceOf[LazyMetadata[Typ, Serialization]]
      case _ => LazyMetadata.empty[Typ, Serialization]
    }
    case _ => LazyMetadata.empty[Typ, Serialization]
  }

  /**
    * providing an empty instance
    */
  def emptyEntityMetadata: EntityMetadata[Serialization]

  /**
    * The serializer used to serialize this EntityMetadata object
    */
  val serializer: SerializationFormat[EntityMetadata[Serialization], Serialization]

  /**
    * the base metadata map using string identifiers to specify the type of metadata (see for example:
    * [[XmlMetadataSerializer.metadataId]])
    */
  val metadata: Map[String, LazyMetadata[_, Serialization]]

  /**
    * Will insert a new [[LazyMetadata]] object into the metadata map
    * @param key - string identifiers to specify the type of metadata (see for example: [[XmlMetadataSerializer.metadataId]])
    * @param lm - the [[LazyMetadata]] object
    * @return - the updated map
    */
  def addReplaceMetadata(key: String, lm: LazyMetadata[_, Serialization]): EntityMetadata[Serialization]

  /**
    * Shorthand version for [[addReplaceMetadata(Failure_Key, LazyMetadata(failure))]]
    * Can be used without knowledge of the correct LazyMetadata implementation (e.g. [[org.silkframework.entity.Entity.copy]]
    * @param failure
    */
  def addFailure(failure: Throwable): EntityMetadata[Serialization]

  /**
    * @return - returning a Throwable for failed Entities
    */
  def failure: LazyMetadata[Throwable, Serialization] = getLazyMetadata[Throwable](EntityMetadata.FAILURE_KEY)(classOf[Throwable])

  /* Map implementation */
  override def +[B1 >: LazyMetadata[_, Serialization]](kv: (String, B1)): Map[String, B1] = metadata.+[B1](kv)

  override def get(key: String): Option[LazyMetadata[_, Serialization]] = metadata.get(key)

  override def iterator: Iterator[(String, LazyMetadata[_, Serialization])] = metadata.iterator

  override def -(key: String): Map[String, LazyMetadata[_, Serialization]] = metadata.-(key)

  override def empty: EntityMetadata[Serialization] = this.emptyEntityMetadata
}

object EntityMetadata extends Serializable {
  val FAILURE_KEY: String = "failure_metadata"
  val METADATA_KEY : String = "entity_metadata"

  /* EntityMetadata registry for different serialization formats */
  //NOTE: make to call [[registerNewEntityMetadataFormat]] for each implementation of [[EntityMetadata]] to register it properly

  private val EntityMetadataFormatMap = new mutable.HashMap[Class[_], EntityMetadata[_]]()

  def registerNewEntityMetadataFormat(em: EntityMetadata[_]): Unit = EntityMetadataFormatMap.get(em.serTag) match{
    case None => EntityMetadataFormatMap.put(em.serTag, em)
    case Some(_) =>
  }

  def empty[Ser](implicit cls: Class[Ser]): Option[EntityMetadata[Ser]] =
    EntityMetadataFormatMap.get(cls).map(_.emptyEntityMetadata.asInstanceOf[EntityMetadata[Ser]])

}