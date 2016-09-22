package org.silkframework.execution.local

import org.silkframework.entity.Entity
import org.silkframework.execution.EntityHolder

/**
  * A local table of entities.
  */
trait EntityTable extends EntityHolder {

  def entities: Traversable[Entity]

}
