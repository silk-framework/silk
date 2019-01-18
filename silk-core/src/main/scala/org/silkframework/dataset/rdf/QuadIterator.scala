package org.silkframework.dataset.rdf

import org.silkframework.entity.Entity

/**
  * Provides an Iterator interface for [[Quad]] containing serialization and [[Entity]] transformation
  */
trait QuadIterator extends Iterator[Quad] {

  /**
    * A close function, forward any close function of the underlying source of this iterator (such as QueryExecution or closeable StatementIterators) if needed
    * Call this once after finishing the consumption of Quads.
    */
  val close: () => Unit

  /**
    * function to indicate that the QuadIterator is or is not empty
    */
  val hasQuad: () => Boolean

  /**
    * Function for provisioning of the next Quad
    */
  val nextQuad: () => Quad

  /**
    * Will serialize the entire content of the Iterator, thereby using it up and finally closing it
    * @param asQuads - indicate whether to serialize the whole Quad (or just the triple)
    */
  def serialize(asQuads: Boolean = true): String

  /**
    * Will generate an Entity for each Quad (using the EntitySchema of [[org.silkframework.execution.local.QuadEntityTable]]
    */
  def getQuadEntities: Traversable[Entity]

  override def hasNext: Boolean = hasQuad()

  override def next(): Quad = nextQuad()
}
