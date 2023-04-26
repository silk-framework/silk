package org.silkframework.plugins.spatial.relation

import org.silkframework.entity.Index
import org.silkframework.plugins.spatial.utils.{Constants, Utils}
import org.silkframework.rule.similarity.SingleValueDistanceMeasure
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "NegateRelationMetric",
  categories = Array("Spatial"),
  label = "Negate relation (NOT)",
  description = "Computes every relation from DE-9IM between two geometries and negates its result."
)
case class NegateMetric(blockingParameter: Double = 1.0, relation: String = "") extends SingleValueDistanceMeasure {

  override def evaluate(str1: String, str2: String, limit: Double): Double = {
    Utils.negatsRelation(str1, str2, limit, relation)
  }

  override def indexValue(str: String, distance: Double, sourceOrTarget: Boolean): Index = {
    Utils.indexGeometriesByEnvelope(str, blockingParameter)
  }
}

object NegateMetric{
  def apply(blockingParameter: Double, metric: SingleValueDistanceMeasure): NegateMetric = {
    metric match{
      case contains: ContainsMetric => NegateMetric(blockingParameter = blockingParameter, relation = Constants.CONTAINS)
      case crosses: CrossesMetric => NegateMetric(blockingParameter = blockingParameter, relation = Constants.CROSSES)
      case disjoint: DisjointMetric => NegateMetric(blockingParameter = blockingParameter, relation = Constants.DISJOINT)
      case equals: EqualsMetric => NegateMetric(blockingParameter = blockingParameter, relation = Constants.EQUALS)
      case intersects: IntersectsMetric => NegateMetric(blockingParameter = blockingParameter, relation = Constants.INTERSECTS)
      case overlaps: OverlapsMetric => NegateMetric(blockingParameter = blockingParameter, relation = Constants.OVERLAPS)
      case touches: TouchesMetric => NegateMetric(blockingParameter = blockingParameter, relation = Constants.TOUCHES)
      case within: WithinMetric => NegateMetric(blockingParameter = blockingParameter, relation = Constants.WITHIN)
      case relates: RelateMetric => NegateMetric(blockingParameter = blockingParameter, relation = relates.relation)
      case negates: NegateMetric => throw new IllegalArgumentException("Double negation is not supported.")
      case _ => throw new IllegalArgumentException("Non relational metrics cannot be negated.")
    }
  }
}
