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
    * A formatter for serializing the Quad (or Triple) in a specific serialization
    */
  val formatter: QuadFormatter

  /**
    * Will generate an Entity for each Quad (using the EntitySchema of [[org.silkframework.execution.local.QuadEntityTable]]
    */
  def asEntities: Traversable[Entity]

  /**
    * Providing a [[TripleIterator]] by ignoring the context of each Quad
    */
  def asTriples: TripleIterator

  override def hasNext: Boolean = hasQuad()

  override def next(): Quad = nextQuad()

  /**
    * Will serialize the entire content of the Iterator, thereby using it up and finally closing it
    */
  def serialize(): String = {
    val sb = new StringBuilder()
    sb.append(formatter.header)
    while(hasQuad()){
      sb.append(nextQuad().serialize(formatter))
      // line end
      sb.append("\n")
    }
    sb.append(formatter.footer)
    // to string
    sb.toString()
  }
}
