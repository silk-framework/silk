package org.silkframework.plugins.dataset.hierarchical

import org.silkframework.dataset.TypedProperty

trait HierarchicalEntityWriter {

  def startEntity(): Unit

  def endEntity(): Unit

  def startProperty(property: TypedProperty, numberOfValues: Int): Unit

  def endProperty(property: TypedProperty): Unit

  def writeValue(value: Seq[String], property: TypedProperty): Unit

  def close(): Unit

}