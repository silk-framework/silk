package de.fuberlin.wiwiss.silk.linkagerule.similarity

import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.config.Prefixes
import xml.Node
import de.fuberlin.wiwiss.silk.linkagerule.{Index, Operator}

/**
 * An operator computes the similarity between two entities.
 * It is the base class of aggregations and comparisons.
 */
trait SimilarityOperator extends Operator {
  val required: Boolean

  val weight: Int

  /**
   * Computes the similarity between two entities.
   *
   * @param entities The entities to be compared.
   * @param limit Only returns values if the confidence is higher than the limit
   *
   * @return The confidence as a value between -1.0 and 1.0.
   *         None, if no similarity could be computed.
   */
  def apply(entities: DPair[Entity], limit: Double = 0.0): Option[Double]

  /**
   * Indexes an entity.
   *
   * @param entity The entity to be indexed
   * @param limit The confidence limit.
   *
   * @return A set of (multidimensional) indexes. Entities within the threshold will always get the same index.
   */
  def index(entity: Entity, limit: Double): Index

  def toXML(implicit prefixes: Prefixes): Node
}

object SimilarityOperator {
  def fromXML(nodes: Seq[Node])(implicit prefixes: Prefixes, globalThreshold: Option[Double]): Seq[SimilarityOperator] = {
    nodes.collect {
      case node@ <Aggregate>{_*}</Aggregate> => Aggregation.fromXML(node)
      case node@ <Compare>{_*}</Compare> => Comparison.fromXML(node)
    }
  }
}
