package org.silkframework.dataset.rdf

import org.silkframework.dataset.DataSource
import org.silkframework.entity.Entity

/**
  * Abstracts the quad interface of a construct query result as Iterator
  * @param hasQuad - indicating the existence of another quad
  * @param nextQuad - provisions the next quad
  */
class QuadIterator(
   hasQuad: () => Boolean,
   nextQuad: () => Quad
 ) extends Iterator[Quad] {

  override def hasNext: Boolean = hasQuad()

  override def next(): Quad = nextQuad()

  def serialize(asQuads: Boolean = true): String = {
    val sb = new StringBuilder()
    while(hasNext){
      sb.append(next().serialize(asQuads))
      // line end
      sb.append("\n")
    }
    // to string
    sb.toString()
  }

  def serializeTriples: String = serialize(false)

  def getQuadEntities: Traversable[Entity] = {
    var count = 0L
    this.toTraversable.map( quad => {
      count += 1
      quad.toEntity(Some(DataSource.URN_NID_PREFIX + count))
    })
  }
}
