package de.fuberlin.wiwiss.silk.execution

import de.fuberlin.wiwiss.silk.entity.{Path, Link, Index, Entity}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.cache.Partition
import de.fuberlin.wiwiss.silk.util.DPair
import scala.math.{min, max, abs}

/**
 * The execution method determines how a linkage rule is executed.
 */
trait ExecutionMethod {

  /**
   * Generates an index for a single entity.
   */
  def indexEntity(entity: Entity, rule: LinkageRule): Index

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
  def apply(): ExecutionMethod = new MultiBlockExecutionMethod()
}

/**
 * Full execution method.
 */
class FullExecutionMethod extends ExecutionMethod {
  def indexEntity(entity: Entity, rule: LinkageRule): Index = Index.default
}

/**
 * MultiBlock execution method.
 */
class MultiBlockExecutionMethod extends ExecutionMethod {
  def indexEntity(entity: Entity, rule: LinkageRule): Index = rule.index(entity, 0.0)
}

/**
 * Traditional blocking.
 *
 * @param blockingKey The blocking key, e.g., rdfs:label
 */
class BlockingExecutionMethod(blockingKey: Path) extends ExecutionMethod {

  override def indexEntity(entity: Entity, rule: LinkageRule): Index = {
    val values = entity.evaluate(blockingKey)
    Index.oneDim(values.map(_.hashCode))
  }

}

/**
 * Multi-pass blocking.
 *
 * @param blockingKeys The blocking keys.
 */
class MultiPassBlockingExecutionMethod(blockingKeys: Set[Path]) extends ExecutionMethod {

  override def indexEntity(entity: Entity, rule: LinkageRule): Index = {
    val values = blockingKeys.flatMap(key => entity.evaluate(key))
    Index.oneDim(values.map(_.hashCode))
  }

}

/**
 * Blocking using a composite key built from two single keys.
 */
class CompositeBlockingExecutionMethod(blockingKey1: Path, blockingKey2: Path) extends ExecutionMethod {

  override def indexEntity(entity: Entity, rule: LinkageRule): Index = {
    Index.oneDim(
      for(v1 <- entity.evaluate(blockingKey1);
          v2 <- entity.evaluate(blockingKey2)) yield {
        (v1 + v2).hashCode
      }
    )
  }

}

//TODO sorted blocks and sortedneighbourhood

//class SortedNeighbourhoodExecutionMethod(blockingKey: Path) extends ExecutionMethod {
//
//  private val minChar = '0'
//  private val maxChar = 'z'
//  private val indexSize = 5 //Maximum number of chars that will be indexed
//
//  override def indexEntity(entity: Entity, rule: LinkageRule): Index = {
//    val values = entity.evaluate(blockingKey)
//    Index.oneDim(
//      indices = values.map(indexValue),
//      size = BigInt(maxChar - minChar + 1).pow(indexSize).toInt
//    )
//  }
//
//  override def comparisonPairs(sourcePartition: Partition, targetPartition: Partition, full: Boolean) = {
//    sourcePartition.entities
//  }
//
//  private def indexValue(value: String): Int = {
//    def combine(index: Int, char: Char) = {
//      val croppedChar = min(max(char, minChar), maxChar)
//      index * (maxChar - minChar + 1) + croppedChar - minChar
//    }
//
//    value.foldLeft(0)(combine)
//  }
//}
