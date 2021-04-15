package org.silkframework.plugins.dataset.hierarchical

import org.silkframework.dataset.TypedProperty

import java.io.Closeable

/**
  * Writes entities into an hierarchical output, such as JSON or XML.
  */
trait HierarchicalEntityWriter extends Closeable {

  /**
    * Open this writer.
    * Must be called once at the beginning.
    */
  def open(): Unit

  /**
    * Adds a new entity.
    * Must be followed by calls to [[startProperty]] to write property values.
    */
  def startEntity(): Unit

  /**
    * Called after all properties of the current entity have been written.
    */
  def endEntity(): Unit

  /**
    * Adds a new property.
    * Must be followed by either [[writeValue]] for writing literal values or [[startEntity]] for writing object values.
    */
  def startProperty(property: TypedProperty, numberOfValues: Int): Unit

  /**
    * Called after all values of the current property have been written.
    */
  def endProperty(property: TypedProperty): Unit

  /**
    * Writes a literal value.
    */
  def writeValue(value: Seq[String], property: TypedProperty): Unit

  /**
    * Closes this writer and releases resources.
    */
  def close(): Unit

}