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

  def serialize(asQuads: Boolean = true, formatter: QuadFormatter = new NTriplesQuadFormatter): String = {
    val sb = new StringBuilder()
    sb.append(formatter.header)
    while(hasNext){
      sb.append(next().serialize(asQuads, formatter))
      // line end
      sb.append("\n")
    }
    sb.append(formatter.footer)
    // to string
    sb.toString()
  }

  def serializeTriples(formatter: QuadFormatter = new NTriplesQuadFormatter): String = serialize(asQuads = false, formatter = formatter)

  def getQuadEntities: Traversable[Entity] = {
    var count = 0L
    this.toTraversable.map( quad => {
      count += 1
      quad.toEntity(Some(DataSource.URN_NID_PREFIX + count))
    })
  }
}
