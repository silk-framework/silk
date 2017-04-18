package org.silkframework.dataset

import org.silkframework.entity.{TypedPath, ValueType}

/**
 * An entity sink implements methods to write entities, e.g. the result of a transformation task.
 */
trait EntitySink extends DataSink {
  /**
   * Initializes this writer.
   *
   * @param properties The list of properties of the entities to be written.
   */
  def open(properties: Seq[TypedProperty]): Unit

  def openWithTypedPath(typedPaths: Seq[TypedPath]): Unit = {
    val properties = typedPathToTypedProperties(typedPaths)
    open(properties)
  }

  protected def typedPathToTypedProperties(typedPaths: Seq[TypedPath]): Seq[TypedProperty] = {
    typedPaths.map(tp => TypedProperty(tp.path.propertyUri.get.toString, tp.valueType))
  }

  /**
   * Writes a new entity.
   *
   * @param subject The subject URI of the entity.
   * @param values The list of values of the entity. For each property that has been provided
   *               when opening this writer, it must contain a set of values.
   */
  def writeEntity(subject: String, values: Seq[Seq[String]]): Unit
}

case class TypedProperty(propertyUri: String, valueType: ValueType)