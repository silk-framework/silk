package de.fuberlin.wiwiss.silk.linkspec.similarity

import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.linkspec.input.Input
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.util.{Identifier, SourceTargetPair}
import de.fuberlin.wiwiss.silk.linkspec.Operator

/**
 * A comparison computes the similarity of two inputs.
 */
case class Comparison(id: Identifier = Operator.generateId, required: Boolean = false, threshold: Double = 0.0, weight: Int = 1,
                      inputs: SourceTargetPair[Input], metric: DistanceMeasure) extends SimilarityOperator {
  /**
   * Computes the similarity between two instances.
   *
   * @param instances The instances to be compared.
   * @param limit The confidence limit.
   *
   * @return The confidence as a value between -1.0 and 1.0.
   */
  override def apply(instances: SourceTargetPair[Instance], limit: Double): Option[Double] = {
    val values1 = inputs.source(instances)
    val values2 = inputs.target(instances)

    if (values1.isEmpty || values2.isEmpty)
      None
    else {
      val distance = metric(values1, values2, threshold * (1.0 - limit))

      if (distance == 0.0 && threshold == 0.0)
        Some(1.0)
      else if (distance <= threshold)
        Some(1.0 - distance / threshold)
      else if (!required)
        Some(-1.0)
      else
        None
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
  override def index(instance: Instance, limit: Double): Set[Seq[Int]] = {
    val instancePair = SourceTargetPair.fill(instance)

    val values = inputs.source(instancePair) ++ inputs.target(instancePair)

    val distanceLimit = threshold * (1.0 - limit)

    metric.index(values, distanceLimit)
  }

  /**
   * The number of blocks in each dimension of the index.
   */
  override def blockCounts(limit: Double) = {
    metric.blockCounts(threshold * (1.0 - limit))
  }

  override def toXML(implicit prefixes: Prefixes) = metric match {
    case DistanceMeasure(strategy, params) => {
      <Compare id={id} required={required.toString} weight={weight.toString} metric={strategy} threshold={threshold.toString}>
        {inputs.source.toXML}{inputs.target.toXML}{params.map {
        case (name, value) => <Param name={name} value={value}/>
      }}
      </Compare>
    }
  }
}
