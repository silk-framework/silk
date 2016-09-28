package org.silkframework.execution

import org.silkframework.entity.EntitySchema

/**
  * Holds entities that are exchanged between tasks.
  */
trait EntityHolder {

  def entitySchema: EntitySchema

}
