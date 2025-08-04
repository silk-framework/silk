package org.silkframework.rule.execution

import org.silkframework.cache.{EntityCache, Partition}
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
   * Iterates over all pairs of partitions that should be compared.
   *
   * @param sourceCache The source cache.
   * @param targetCache The target cache.
   * @param full If true, all pairs of partitions are compared.
   *             If false, it is assumed that the source and target are the same and no self-references should be generated.
   */
  def partitionPairs(sourceCache: EntityCache, targetCache: EntityCache, full: Boolean): Iterable[PartitionPair] = {
    for {
      sourceBlock <- 0 until sourceCache.blockCount
      targetBlock <- 0 until targetCache.blockCount
      sourcePartitionIndex <- 0 until sourceCache.partitionCount(sourceBlock)
      targetStart = if (full) 0 else sourcePartitionIndex
      targetPartitionIndex <- targetStart until targetCache.partitionCount(targetBlock)
    } yield {
      val fullComparison = full || sourcePartitionIndex != targetPartitionIndex
      new PartitionPair(
        sourceCache, targetCache,
        sourceBlock, targetBlock,
        sourcePartitionIndex, targetPartitionIndex,
        fullComparison
      )
    }
  }

  /**
   * Generates comparison pairs from two partitions.
   *
   * @param sourcePartition The source partition.
   * @param targetPartition The target partition.
   * @param full If true, all pairs of partitions are compared.
   *             If false, it is assumed that the source and target are the same and no self-references should be generated.
   */
  def comparisonPairs(sourcePartition: Partition, targetPartition: Partition, full: Boolean): Iterable[DPair[Entity]] = {
    for {
      s <- 0 until sourcePartition.size
      t <- (if (full) 0 else s + 1) until targetPartition.size
      if sourcePartition.indices(s) matches targetPartition.indices(t)
    } yield {
      DPair(sourcePartition.entities(s), targetPartition.entities(t))
    }
  }

  /**
   * A pair of partitions that should be compared.
   *
   * @param sourceCache The source cache.
   * @param targetCache The target cache.
   * @param sourceBlock The block index within the source cache.
   * @param targetBlock The block index within the target cache.
   * @param sourcePartitionIndex The partition index within the source block.
   * @param targetPartitionIndex The partition index within the target block.
   * @param fullComparison If true, all pairs of partitions are compared.
   *                       If false, it is assumed that the source and target are the same and no self-references should be generated.
   */
  class PartitionPair(sourceCache: EntityCache, targetCache: EntityCache,
                      sourceBlock: Int, targetBlock: Int,
                      sourcePartitionIndex: Int, targetPartitionIndex: Int,
                      fullComparison: Boolean) {
    def comparisonPairs: Iterable[DPair[Entity]] = {
      val sourcePartition = sourceCache.read(sourceBlock, sourcePartitionIndex)
      val targetPartition = targetCache.read(targetBlock, targetPartitionIndex)
      ExecutionMethod.this.comparisonPairs(sourcePartition, targetPartition, fullComparison)
    }
  }
}

object ExecutionMethod {
  /** Returns the default execution method. */
  def apply(): ExecutionMethod = new methods.MultiBlock()
}