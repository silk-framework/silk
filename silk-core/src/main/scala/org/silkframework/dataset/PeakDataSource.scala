package org.silkframework.dataset

import org.silkframework.entity.{Entity, EntitySchema}

/**
  * A dataset extension that allows to retrieve example values for a specific path quickly.
  */
trait PeakDataSource { DataSource =>
  def peak(entityDescription: EntitySchema, limit: Int): Traversable[Entity]
}
