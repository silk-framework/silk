package org.silkframework.execution.local

import org.silkframework.entity.Entity
import org.silkframework.execution.EntityHolder

/**
  * A local table of entities.
  */
trait EntityTable extends EntityHolder {

  //TODO do we need this trait anymore, maybe rename it
  /**
    * get head Entity
    */
  override def headOption: Option[Entity] = this.entities.headOption
}
