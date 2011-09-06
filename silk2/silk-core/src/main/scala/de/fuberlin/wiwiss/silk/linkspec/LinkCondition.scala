package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.config.Prefixes
import math.abs
import similarity.SimilarityOperator

/**
 * A Link Condition specifies the conditions which must hold true so that a link is generated between two instances.
 */
case class LinkCondition(rootOperator: Option[SimilarityOperator] = None) {
  /**
   * Computes the similarity between two instances.
   *
   * @param instances The instances to be compared.
   * @param limit If the confidence is below this limit, it will be capped to -1.0.
   *
   * @return The confidence as a value between -1.0 and 1.0.
   *         -1.0 for definitive non-matches.
   *         +1.0 for definitive matches.
   */
  def apply(instances: SourceTargetPair[Instance], limit: Double): Double = {
    rootOperator match {
      case Some(operator) => operator(instances, limit).getOrElse(-1.0)
      case None => -1.0
    }
  }

  /**
   * Indexes an instance.
   *
   * @param instance The instance to be indexed
   * @param limit The confidence limit.
   *
   * @return A set of (multidimensional) indexes. Instances within the threshold will always get the same index.
   */
  def index(instance: Instance, limit: Double = 0.0): Set[Int] = {
    rootOperator match {
      case Some(operator) => {
        val indexes = operator.index(instance, limit)

        //Convert the index vectors to scalars in the range [0, Int.MaxValue]
        for (index <- indexes) yield {
          val flatIndex = (index zip operator.blockCounts(limit)).foldLeft(0) {
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
    rootOperator match {
      case Some(operator) => operator.blockCounts(threshold).foldLeft(1)(_ * _)
      case None => 1
    }
  }

  /**
   * Serializes this Link Condition as XML.
   */
  def toXML(implicit prefixes: Prefixes) = {
    <LinkCondition>
      {rootOperator.toList.map(_.toXML)}
    </LinkCondition>
  }
}
