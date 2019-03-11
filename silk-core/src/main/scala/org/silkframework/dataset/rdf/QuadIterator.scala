package org.silkframework.dataset.rdf

import org.silkframework.dataset.DataSource
import org.silkframework.entity.Entity

/**
  * Provides an Iterator interface for [[Quad]] containing serialization and [[Entity]] transformation
  */
trait QuadIterator extends ClosableIterator[Quad] {
  /**
    * Should close and clean up all used resources. This will be called at most once.
    */
  protected def closeResources(): Unit

  private var isClosed = false

  /** Idempotent close */
  override def close(): Unit = synchronized {
    if(!isClosed) {
      isClosed = true
      closeResources()
    }
  }

  /**
    * Will generate an Entity for each Quad (using the EntitySchema of [[org.silkframework.execution.local.QuadEntityTable]]
    */
  def asQuadEntities: Traversable[Entity] = {
    var count = 0L
    this.toTraversable.map( quad => {
      count += 1
      quad.toQuadEntity(Some(DataSource.URN_NID_PREFIX + count))
    })
  }
}
