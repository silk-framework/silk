package de.fuberlin.wiwiss.silk.rule.evaluation

import de.fuberlin.wiwiss.silk.rule.LinkageRule
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.entity.{Index, Entity}
import de.fuberlin.wiwiss.silk.rule.similarity.{Comparison, Aggregation, SimilarityOperator}
import de.fuberlin.wiwiss.silk.rule.evaluation.DetailedIndex._
import scala.Some

/**
 * In addition to the overall index built from an linkage rule, this indexer also retains
 * all sub indices which have been build from all similarity operators in the linkage rule.
 * This can be used for closer inspection of the indexing e.g. for debugging and performance optimization.
 */
object DetailedIndexer {
  def apply(rule: LinkageRule, entity: Entity, limit: Double = -1.0): DetailedIndex = {
    val rootIndex = for(rootOp <- rule.operator) yield indexOperator(rootOp, entity, limit)

    DetailedIndex(rootIndex.map(_.index).getOrElse(Index.empty), entity, rootIndex)
  }

  def indexOperator(op: SimilarityOperator, entity: Entity, limit: Double): OperatorIndex = op match {
    case aggregation: Aggregation => indexAggregation(aggregation, entity, limit)
    case comparison: Comparison => indexComparison(comparison, entity, limit)
  }

  def indexAggregation(agg: Aggregation, entity: Entity, limit: Double): AggregationIndex = {
    val totalWeights = agg.operators.map(_.weight).sum

    //Compute the detailed indices for each child operator
    var foundRequiredEmptyIndex = false
    val detailedIndices = {
      for (op <- agg.operators if op.indexing) yield {
        val opLimit = agg.aggregator.computeThreshold(limit, op.weight.toDouble / totalWeights)
        val index = indexOperator(op, entity, opLimit)

        if (op.required && index.index.isEmpty)
          foundRequiredEmptyIndex = true

        index
      }
    }.filterNot(_.index.isEmpty)

    //Compute the overall index from the child operator indices
    val overallIndex =
      if (detailedIndices.isEmpty || foundRequiredEmptyIndex)
        Index.empty
      else
        detailedIndices.map(_.index).reduceLeft[Index](agg.aggregator.combineIndexes(_, _))

    AggregationIndex(overallIndex, agg, detailedIndices)
  }

  def indexComparison(cmp: Comparison, entity: Entity, limit: Double): ComparisonIndex = {
    val entityPair = DPair.fill(entity)
    val values = cmp.inputs.source(entityPair) ++ cmp.inputs.target(entityPair)
    val distanceLimit = cmp.threshold * (1.0 - limit)

    ComparisonIndex(cmp.metric.index(values, distanceLimit), values, cmp)
  }
}
