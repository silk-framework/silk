package org.silkframework.plugins.dataset.rdf

import org.apache.jena.riot.Lang
import org.silkframework.dataset.DataSource
import org.silkframework.dataset.rdf.{Quad, QuadFormatter, QuadIterator, TripleIterator}
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


  def asTriples: TripleIterator = new TripleIteratorImpl(hasQuad, () => nextQuad().toTriple, close, formatter)

  def asEntities: Traversable[Entity] = {
    var count = 0L
    this.toTraversable.map( quad => {
      count += 1
      quad.toEntity(Some(DataSource.URN_NID_PREFIX + count))
    })
  }
}

object QuadIteratorImpl{

  def apply(
    hasQuad: () => Boolean,
    nextQuad: () => Quad,
    close: () => Unit = () => Unit,
    formatter: QuadFormatter
 ): QuadIteratorImpl = {
    new QuadIteratorImpl(hasQuad, nextQuad, close, formatter)
  }

  def apply(
   hasQuad: () => Boolean,
   nextQuad: () => Quad,
   close: () => Unit,
   serialization: Lang
 ): QuadIteratorImpl = {
    apply(hasQuad, nextQuad, close, serialization.getContentType.getContentType)
  }


  def apply(
   hasQuad: () => Boolean,
   nextQuad: () => Quad,
   close: () => Unit,
   mediaType: String
 ): QuadIteratorImpl = {
    apply(hasQuad, nextQuad, close, QuadFormatter.getSuitableFormatter(mediaType)
      .getOrElse(throw new IllegalArgumentException("No QuadFormatter found for media type " + mediaType)))
  }
}
