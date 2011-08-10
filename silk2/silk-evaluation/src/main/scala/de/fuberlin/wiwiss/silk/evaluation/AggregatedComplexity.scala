package de.fuberlin.wiwiss.silk.evaluation

/**
 * Statistics about the complexity of multiple link conditions.
 *
 * @param comparisonCount The number of comparisons in the condition.
 * @param transformationCount The number of transformations in the condition.
 */
case class AggregatedComplexity(comparisonCount: VariableStatistic, transformationCount: VariableStatistic)

/**
 * Aggregates the complexity of multiple link conditions.
 */
object AggregatedComplexity {
  def apply(complexities: Traversable[LinkConditionComplexity]): AggregatedComplexity = {
    AggregatedComplexity(
      comparisonCount = VariableStatistic(complexities.map(_.comparisonCount.toDouble)),
      transformationCount = VariableStatistic(complexities.map(_.transformationCount.toDouble))
    )
  }
}