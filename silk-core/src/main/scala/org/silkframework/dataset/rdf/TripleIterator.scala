package org.silkframework.dataset.rdf

import org.silkframework.dataset.DataSource
import org.silkframework.entity.Entity
import org.silkframework.runtime.iterator.CloseableIterator

trait TripleIterator extends QuadIterator with CloseableIterator[Triple] {
  def asTripleEntities: CloseableIterator[Entity] = {
    var count = 0L
    this.map( triple => {
      count += 1
      triple.toTripleEntity(Some(DataSource.URN_NID_PREFIX + count))
    })
  }
}
