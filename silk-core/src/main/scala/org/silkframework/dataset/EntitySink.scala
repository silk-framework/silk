package org.silkframework.dataset

import org.silkframework.entity.{TypedPath, ValueType}
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
  def openTable(typeUri: Uri, properties: Seq[TypedProperty]): Unit

  def openTableWithPaths(typeUri: Uri, typedPaths: Seq[TypedPath]): Unit = {
    val properties = typedPaths.map(tp => tp.property.getOrElse(throw new RuntimeException("Typed path is neither a simple forward or backward path: " + tp)))
    openTable(typeUri, properties)
  }

  /**
    * Closes writing a table of entities.
    */
  def closeTable()

  /**
   * Writes a new entity.
   *
   * @param subject The subject URI of the entity.
   * @param values The list of values of the entity. For each property that has been provided
   *               when opening this writer, it must contain a set of values.
   */
  def writeEntity(subject: String, values: Seq[Seq[String]]): Unit
}

case class TypedProperty(propertyUri: String, valueType: ValueType, isBackwardProperty: Boolean)