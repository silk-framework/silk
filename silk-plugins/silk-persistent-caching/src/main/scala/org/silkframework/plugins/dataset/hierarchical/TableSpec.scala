package org.silkframework.plugins.dataset.hierarchical

import org.silkframework.dataset.TypedProperty

/**
  * A single table that is written by the hierarchical sink.
  *
  * @param properties The properties of the table.
  * @param propertyIndices The indices of the properties that define the order of the written values.
  */
case class TableSpec(properties: IndexedSeq[TypedProperty], propertyIndices: Seq[Int])

object TableSpec {

  def create(properties: Seq[TypedProperty], sortByAttributes: Boolean): TableSpec = {
    val indexedProperties = properties.toIndexedSeq
    val propertyIndices =
      if(sortByAttributes) {
        indexedProperties.zipWithIndex.sortBy(!_._1.isAttribute).map(_._2)
      } else {
        indexedProperties.indices
      }
    TableSpec(indexedProperties, propertyIndices)
  }

}