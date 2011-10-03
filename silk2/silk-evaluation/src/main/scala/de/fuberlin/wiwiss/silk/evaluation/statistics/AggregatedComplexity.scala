package de.fuberlin.wiwiss.silk.evaluation.statistics


/**
 * Statistics about the complexity of multiple linkage rules.
 *
 * @param comparisonCount The number of comparisons in the condition.
 * @param transformationCount The number of transformations in the condition.
 */
case class AggregatedComplexity(comparisonCount: VariableStatistic, transformationCount: VariableStatistic)

/**
 * Aggregates the complexity of multiple linkage rules.
 */
object AggregatedComplexity {
  def apply(complexities: Traversable[LinkageRuleComplexity]): AggregatedComplexity = {
    AggregatedComplexity(
      comparisonCount = VariableStatistic(complexities.map(_.comparisonCount.toDouble)),
      transformationCount = VariableStatistic(complexities.map(_.transformationCount.toDouble))
    )
  }
}