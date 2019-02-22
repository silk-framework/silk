package org.silkframework.execution

import org.silkframework.entity.Entity

/**
  * An [[EntityHolder]] that on top of the entity [[Traversable]] offers an entity iterator as optimization.
  */
trait EntityHolderWithEntityIterator extends EntityHolder {
  /** An [[Iterator]] of [[Entity]] objects contemplating the entities method.
    * This should be guaranteed to have a low memory footprint. */
  def entityIterator: Iterator[Entity]
}
