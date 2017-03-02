package org.silkframework.dataset

import org.silkframework.entity.{Entity, EntitySchema}

/**
  * A dataset extension that allows to retrieve example values for a specific path quickly.
  */
trait PeakDataSource { this: DataSource =>
  /** Default peak implementation that should work with all sources that offer fast "random access". */
  def peak(entitySchema: EntitySchema, limit: Int): Traversable[Entity] = {
    retrieve(entitySchema, None) filter { entity =>
      entity.values.forall(v => v.nonEmpty)
    } take limit
  }
}
