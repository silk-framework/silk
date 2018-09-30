package org.silkframework.entity

import java.util.concurrent.{ConcurrentHashMap, ConcurrentSkipListSet}
import java.util.concurrent.atomic.AtomicLong

import scala.collection.JavaConverters._

private[entity] object EntitySchemaRegistry extends Serializable {

  /**
    * This global map holds the original schema objects that are created on the driver.
    * It keeps weak references to these objects so that schemata can be garbage-collected
    * once the RDDs and user-code that reference them are cleaned up.
    */
  private val schemata = new ConcurrentHashMap[Long, java.lang.ref.WeakReference[EntitySchema]]

  private val hashCodes = new ConcurrentSkipListSet[Int]()

  private[this] val nextId = new AtomicLong(0L)

  /**
    * Returns a globally unique ID for a new [[EntitySchema]].
    * Note: Once you copy the [[EntitySchema]] the ID is no longer unique.
    */
  private def newId: Long = nextId.getAndIncrement

  def getId(schema: EntitySchema): Option[Long] = schemata.asScala.find(s => s._2.get() == schema).map(_._1)

  private def insertNewSchema(newSchema: EntitySchema): Long ={
    val nid = newId
    schemata.put(nid, new java.lang.ref.WeakReference[EntitySchema](newSchema))
    hashCodes.add(newSchema.hashCode())
    nid
  }

  /**
    * Registers an [[EntitySchema]] created on the driver such that it can be used on the executors.
    *
    * If an [[EntitySchema]] with the same ID was already registered, this does nothing instead
    * of overwriting it. We will never register same schema twice, this is just a sanity check.
    */
  def register(newSchema: EntitySchema): Long = {
    if(! hashCodes.contains(newSchema.hashCode()))
      return insertNewSchema(newSchema)

    schemata.asScala.find(s => s._2.get() == newSchema) match{
      case Some(s) => s._1
      case None => insertNewSchema(newSchema)
    }
  }

  /**
    * Unregisters the [[EntitySchema]] with the given ID, if any.
    */
  def remove(id: Long): Unit = {
    schemata.remove(id)
  }

  /**
    * Returns the [[EntitySchema]] registered with the given ID, if any.
    */
  def get(id: Long): Option[EntitySchema] = {
    Option(schemata.get(id)).map { ref =>
      // Since we are storing weak references, we must check whether the underlying data is valid.
      val schema = ref.get
      if (schema eq null) {
        throw new IllegalStateException(s"Attempted to access garbage collected EntitySchema $id")
      }
      schema
    }
  }

  /**
    * Clears all registered [[EntitySchema]]s. For testing only.
    */
  def clear(): Unit = {
    schemata.clear()
  }

  /**
    * Looks for a registered schema by schema type.
    */
  private[entity] def getSchemaByType(typ: String): Option[EntitySchema] = {
    schemata.values().asScala.find { ref =>
      val schema = ref.get
      schema != null && schema.typeUri.toString == typ
    }.map(_.get)
  }
}
