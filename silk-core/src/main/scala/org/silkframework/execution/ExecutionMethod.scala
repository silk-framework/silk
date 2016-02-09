package org.silkframework.execution

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
  def comparisonPairs(sourcePartition: Partition, targetPartition: Partition, full: Boolean) = new Traversable[DPair[Entity]] {
    /**
     * Iterates through all comparison pairs
     */
    def foreach[U](f: DPair[Entity] => U) {
      //Iterate over all entities in the source partition
      var s = 0
      while(s < sourcePartition.size) {
        //Iterate over all entities in the target partition
        var t = if (full) 0 else s + 1
        while(t < targetPartition.size) {
          //Check if the indices match
          if((sourcePartition.indices(s) matches targetPartition.indices(t))) {
            //Yield entity pair
            f(DPair(sourcePartition.entities(s), targetPartition.entities(t)))
          }
          t += 1
        }
        s += 1
      }
    }
  }
}

object ExecutionMethod {
  /** Returns the default execution method. */
  def apply(): ExecutionMethod = new methods.MultiBlock()
}