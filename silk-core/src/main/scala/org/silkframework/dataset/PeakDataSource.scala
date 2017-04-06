package org.silkframework.dataset

import org.silkframework.entity.{Entity, EntitySchema}

/**
  * A dataset extension that allows to retrieve example values for a specific path quickly.
  */
trait PeakDataSource { this: DataSource =>
  /** Default peak implementation that should work with all sources that offer fast "random access".
    * It filters entities that have no input value for any input path. */
  def peak(entitySchema: EntitySchema, limit: Int): Traversable[Entity] = {
    retrieve(entitySchema, Some(limit))
  }
}
