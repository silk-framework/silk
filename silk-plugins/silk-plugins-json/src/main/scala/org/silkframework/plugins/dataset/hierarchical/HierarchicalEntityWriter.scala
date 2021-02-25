package org.silkframework.plugins.dataset.hierarchical

import org.silkframework.dataset.TypedProperty

trait HierarchicalEntityWriter {

  def startEntity(): Unit

  def endEntity(): Unit

  def startArray(size: Int): Unit

  def endArray(): Unit

  def writeField(property: TypedProperty): Unit

  def writeValue(value: Seq[String], property: TypedProperty): Unit

  def writeValue(value: String, property: TypedProperty): Unit

  def close(): Unit

}