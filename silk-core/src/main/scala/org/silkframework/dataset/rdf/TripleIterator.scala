package org.silkframework.dataset.rdf

import org.silkframework.dataset.DataSource
import org.silkframework.entity.Entity

trait TripleIterator extends QuadIterator with Iterator[Triple] {
  def asTripleEntities: Traversable[Entity] = {
    var count = 0L
    this.toTraversable.map( triple => {
      count += 1
      triple.toTripleEntity(Some(DataSource.URN_NID_PREFIX + count))
    })
  }
}
