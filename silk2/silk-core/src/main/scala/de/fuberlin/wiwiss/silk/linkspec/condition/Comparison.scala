package de.fuberlin.wiwiss.silk.linkspec.condition

import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.linkspec.input.Input
import de.fuberlin.wiwiss.silk.config.Prefixes

/**
 * A comparison computes the similarity of two inputs.
 */
case class Comparison(required : Boolean, weight : Int,
                      inputs : SourceTargetPair[Input], metric : SimilarityMeasure) extends Operator
{
  /**
   * Computes the similarity between two instances.
   *
   * @param instances The instances to be compared.
   * @param threshold The similarity threshold.
   *
   * @return The similarity as a value between 0.0 and 1.0. Returns 0.0 if the similarity is lower than the threshold.
   */
  override def apply(instances : SourceTargetPair[Instance], threshold : Double) : Option[Double] =
  {
    val values1 = inputs.source(instances)
    val values2 = inputs.target(instances)

    if(values1.isEmpty || values2.isEmpty)
    {
      None
    }
    else
    {
      Some(metric(values1, values2, threshold))
    }

  }

  /**
   * Indexes an instance.
   *
   * @param instance The instance to be indexed
   * @param threshold The similarity threshold.
   *
   * @return A set of (multidimensional) indexes. Instances within the threshold will always get the same index.
   */
  override def index(instance : Instance, threshold : Double) : Set[Seq[Int]] =
  {
    val values = inputs.source(SourceTargetPair(instance, instance)) ++ inputs.target(SourceTargetPair(instance, instance))

    values.flatMap(value => metric.index(value, threshold)).toSet
  }

  /**
   * The number of blocks in each dimension of the index.
   */
  override val blockCounts = metric.blockCounts

  override def toXML(implicit prefixes : Prefixes) = metric match
  {
    case SimilarityMeasure(id, params) =>
    {
      <Compare required={required.toString} weight={weight.toString} metric={id}>
        { inputs.source.toXML }
        { inputs.target.toXML }
        { params.map{case (name, value) => <Param name={name} value={value} />} }
      </Compare>
    }
  }
}
