package org.silkframework.plugins.dataset.rdf

import org.silkframework.dataset.DataSource
import org.silkframework.dataset.rdf.{QuadFormatter, TripleIterator, Triple}
import org.silkframework.entity.Entity

class TripleIteratorImpl(
  val hasTriple: () => Boolean,
  val nextTriple: () => Triple,
  val close: () => Unit = () => Unit,
  val formatter: QuadFormatter
) extends TripleIterator {

  /**
    * Will generate an Entity for each Quad (using the EntitySchema of [[org.silkframework.execution.local.QuadEntityTable]]
    */
  override def asEntities: Traversable[Entity] = {
    var count = 0L
    this.toTraversable.map( triple => {
      count += 1
      triple.toEntity(Some(DataSource.URN_NID_PREFIX + count))
    })
  }

}
