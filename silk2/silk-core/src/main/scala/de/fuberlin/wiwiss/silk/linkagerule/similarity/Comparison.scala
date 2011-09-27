package de.fuberlin.wiwiss.silk.linkagerule.similarity

import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.linkagerule.input.Input
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.util.{Identifier, DPair}
import de.fuberlin.wiwiss.silk.linkagerule.Operator

/**
 * A comparison computes the similarity of two inputs.
 */
case class Comparison(id: Identifier = Operator.generateId, required: Boolean = false, threshold: Double = 0.0, weight: Int = 1,
                      inputs: DPair[Input], metric: DistanceMeasure) extends SimilarityOperator {
  /**
   * Computes the similarity between two entities.
   *
   * @param entities The entities to be compared.
   * @param limit The confidence limit.
   *
   * @return The confidence as a value between -1.0 and 1.0.
   */
  override def apply(entities: DPair[Entity], limit: Double): Option[Double] = {
    val values1 = inputs.source(entities)
    val values2 = inputs.target(entities)

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
   * Indexes an entity.
   *
   * @param entity The entity to be indexed
   * @param threshold The similarity threshold.
   *
   * @return A set of (multidimensional) indexes. Entities within the threshold will always get the same index.
   */
  override def index(entity: Entity, limit: Double): Set[Seq[Int]] = {
    val entityPair = DPair.fill(entity)

    val values = inputs.source(entityPair) ++ inputs.target(entityPair)

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
    case DistanceMeasure(func, params) => {
      <Compare id={id} required={required.toString} weight={weight.toString} metric={func} threshold={threshold.toString}>
        {inputs.source.toXML}{inputs.target.toXML}{params.map {
        case (name, value) => <Param name={name} value={value}/>
      }}
      </Compare>
    }
  }
}
