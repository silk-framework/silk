package org.silkframework.rule.execution

import org.silkframework.cache.Partition
import org.silkframework.entity.{Entity, Index}
import org.silkframework.rule.LinkageRule
import org.silkframework.util.DPair

/**
 * The execution method determines how a linkage rule is executed.
 */
trait ExecutionMethod {

  /**
   * Generates an index for a single entity.
   */
  def indexEntity(entity: Entity, rule: LinkageRule, sourceOrTarget: Boolean): Index = Index.default

  /**
   * Generates comparison pairs from two partitions.
   */
  def comparisonPairs(sourcePartition: Partition, targetPartition: Partition, full: Boolean): Iterable[DPair[Entity]] = {
    for {
      s <- 0 until sourcePartition.size
      t <- 0 until (if (full) 0 else s + 1)
      if sourcePartition.indices(s) matches targetPartition.indices(t)
    } yield {
      DPair(sourcePartition.entities(s), targetPartition.entities(t))
    }
  }
}

object ExecutionMethod {
  /** Returns the default execution method. */
  def apply(): ExecutionMethod = new methods.MultiBlock()
}