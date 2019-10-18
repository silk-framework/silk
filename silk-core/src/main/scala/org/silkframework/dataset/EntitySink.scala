package org.silkframework.dataset

import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

/**
 * An entity sink implements methods to write entities, e.g. the result of a transformation task.
 */
trait EntitySink extends DataSink {

  /**
   * Called before a new table of entities of a particular schema is written.
   *
   * @param properties The list of properties of the entities to be written.
   */
  def openTable(typeUri: Uri, properties: Seq[TypedProperty])(implicit userContext: UserContext, prefixes: Prefixes): Unit

  def openTableWithPaths(typeUri: Uri, typedPaths: Seq[TypedPath])(implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    val properties = typedPaths.map(tp => tp.property.getOrElse(throw new RuntimeException("Typed path is neither a simple forward or backward path: " + tp)))
    openTable(typeUri, properties)
  }

  /**
    * Called before a new table of entities of a particular schema is written.
    */
  def openWithEntitySchema(es: EntitySchema)(implicit userContext: UserContext, prefixes: Prefixes): Unit = openTableWithPaths(es.typeUri, es.typedPaths)

  /**
    * Closes writing a table of entities.
    */
  def closeTable()(implicit userContext: UserContext)

  /**
   * Writes a new entity.
   *
   * @param subject The subject URI of the entity.
   * @param values The list of values of the entity. For each property that has been provided
   *               when opening this writer, it must contain a set of values.
   */
  def writeEntity(subject: String, values: Seq[Seq[String]])
                 (implicit userContext: UserContext): Unit

  /**
    * Writes a new entity.
    * @param entity - the entity to write
    */
  def writeEntity(entity: Entity)
                 (implicit userContext: UserContext): Unit = if(! entity.hasFailed)
    writeEntity(entity.uri, entity.values)

  /**
    * Write a complete table based on the provided collection of Entities
    */
  def writeEntities(entities: Traversable[Entity])(implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    entities.headOption match{
      case Some(h) =>
        openWithEntitySchema(h.schema)
        entities.foreach(writeEntity)
        closeTable()
      case None =>
    }
  }
}

/**
  * A single, typed property.
  * May either be a forward or a backward property.
  */
case class TypedProperty(propertyUri: String, valueType: ValueType, isBackwardProperty: Boolean, isAttribute: Boolean = false)
