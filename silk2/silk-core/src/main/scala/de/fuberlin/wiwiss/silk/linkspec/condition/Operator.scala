package de.fuberlin.wiwiss.silk.linkspec.condition

import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

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
   * @param threshold The similarity threshold.
   *
   * @return The similarity as a value between 0.0 and 1.0.
   *         Returns 0.0 if the similarity is lower than the threshold.
   *         None, if no similarity could be computed.
   */
  def apply(instances : SourceTargetPair[Instance], threshold : Double) : Option[Double]

  /**
   * Indexes an instance.
   *
   * @param instance The instance to be indexed
   * @param threshold The similarity threshold.
   *
   * @return A set of (multidimensional) indexes. Instances within the threshold will always get the same index.
   */
  def index(instance : Instance, threshold : Double) : Set[Seq[Int]]

  /**
   * The number of blocks in each dimension of the index.
   */
  //TODO rename to indexSize?
  val blockCounts : Seq[Int]
}
