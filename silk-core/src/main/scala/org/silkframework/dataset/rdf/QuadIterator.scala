package org.silkframework.dataset.rdf

import org.silkframework.entity.Entity

trait QuadIterator extends Iterator[Quad] {

  val close: () => Unit

  val hasQuad: () => Boolean

  val nextQuad: () => Quad

  def serialize(asQuads: Boolean = true): String

  def serializeTriples(): String

  def getQuadEntities: Traversable[Entity]

  override def hasNext: Boolean = hasQuad()

  override def next(): Quad = {
    val quad = nextQuad()
    // close if last Quad
    if(! hasNext)
      close()

    quad
  }
}
