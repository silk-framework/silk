package org.silkframework.plugins.dataset.rdf

import org.silkframework.dataset.DataSource
import org.silkframework.dataset.rdf.{Quad, QuadFormatter, QuadIterator}
import org.silkframework.entity.Entity

/**
  * Abstracts the quad interface of a construct query result as Iterator
  * @param hasQuad - indicating the existence of another quad
  * @param nextQuad - provisions the next quad
  */
class QuadIteratorImpl(
   val hasQuad: () => Boolean,
   val nextQuad: () => Quad,
   val close: () => Unit = () => Unit,
   val formatter: QuadFormatter
 ) extends QuadIterator {

  def serialize(asQuads: Boolean = true): String = {
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

  def serializeTriples(): String = serialize(asQuads = false)

  def getQuadEntities: Traversable[Entity] = {
    var count = 0L
    this.toTraversable.map( quad => {
      count += 1
      quad.toEntity(Some(DataSource.URN_NID_PREFIX + count))
    })
  }
}
