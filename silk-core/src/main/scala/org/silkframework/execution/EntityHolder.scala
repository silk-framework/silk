package org.silkframework.execution

import org.silkframework.entity.SchemaTrait

/**
  * Holds entities that are exchanged between tasks.
  */
trait EntityHolder[+S <: SchemaTrait] {
  def entitySchema: S
}
