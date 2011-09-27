package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.config.Prefixes
import math.abs
import similarity.SimilarityOperator

/**
 * A Linkage Rule specifies the conditions which must hold true so that a link is generated between two entities.
 */
case class LinkageRule(operator: Option[SimilarityOperator] = None) {
  /**
   * Computes the similarity between two entities.
   *
   * @param entities The entities to be compared.
   * @param limit If the confidence is below this limit, it will be capped to -1.0.
   *
   * @return The confidence as a value between -1.0 and 1.0.
   *         -1.0 for definitive non-matches.
   *         +1.0 for definitive matches.
   */
  def apply(entities: DPair[Entity], limit: Double = 0.0): Double = {
    operator match {
      case Some(op) => op(entities, limit).getOrElse(-1.0)
      case None => -1.0
    }
  }

  /**
   * Indexes an entity.
   *
   * @param entity The entity to be indexed
   * @param limit The confidence limit.
   *
   * @return A set of (multidimensional) indexes. Entities within the threshold will always get the same index.
   */
  def index(entity: Entity, limit: Double = 0.0): Set[Int] = {
    operator match {
      case Some(op) => {
        val indexes = op.index(entity, limit)

        //Convert the index vectors to scalars in the range [0, Int.MaxValue]
        for (index <- indexes) yield {
          val flatIndex = (index zip op.blockCounts(limit)).foldLeft(0) {
            case (iLeft, (iRight, blocks)) => iLeft * blocks + iRight
          }

          if (flatIndex == Int.MinValue) 0 else abs(flatIndex)
        }
      }
      case None => Set.empty
    }

  }

  /**
   * The number of blocks in each dimension of the index.
   */
  def blockCount(threshold: Double) = {
    operator match {
      case Some(op) => op.blockCounts(threshold).foldLeft(1)(_ * _)
      case None => 1
    }
  }

  /**
   * Serializes this Link Condition as XML.
   */
  def toXML(implicit prefixes: Prefixes) = {
    <LinkageRule>
      {operator.toList.map(_.toXML)}
    </LinkageRule>
  }
}
