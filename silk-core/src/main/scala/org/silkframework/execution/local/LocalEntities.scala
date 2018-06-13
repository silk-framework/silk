package org.silkframework.execution.local

import org.silkframework.entity.Entity
import org.silkframework.execution.EntityHolder

/**
  * A local table of entities.
  */
trait LocalEntities extends EntityHolder {

  /**
    * get head Entity
    */
  override def headOption: Option[Entity] = this.entities.headOption
}
