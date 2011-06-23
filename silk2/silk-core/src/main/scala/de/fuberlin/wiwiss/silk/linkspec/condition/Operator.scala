package de.fuberlin.wiwiss.silk.linkspec.condition

import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.config.Prefixes
import xml.Node

/**
 * An operator computes the similarity between two instances.
 * It is the base class of aggregations and comparisons.
 */
trait Operator
{
  val required : Boolean

  val weight : Int

  /**
   * Computes the similarity between two instances.
   *
   * @param instances The instances to be compared.
   * @param limit Only returns values if the confidence is higher than the limit
   *
   * @return The confidence as a value between -1.0 and 1.0.
   *         None, if no similarity could be computed.
   */
  def apply(instances : SourceTargetPair[Instance], limit : Double = 0.0) : Option[Double]

  /**
   * Indexes an instance.
   *
   * @param instance The instance to be indexed
   * @param limit The confidence limit.
   *
   * @return A set of (multidimensional) indexes. Instances within the threshold will always get the same index.
   */
  def index(instance : Instance, limit : Double) : Set[Seq[Int]]

  /**
   * The number of blocks in each dimension of the index.
   */
  //TODO rename to indexSize?
  def blockCounts(limit : Double) : Seq[Int]

  def toXML(implicit prefixes : Prefixes) : Node
}
