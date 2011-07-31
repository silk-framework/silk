package de.fuberlin.wiwiss.silk.evaluation

/**
 * The aggregated result of multiple evaluations
 */
case class AggregatedEvaluationResult(fMeasure: VariableStatistic, mcc: VariableStatistic, score: VariableStatistic)

/**
 * Aggregates multiple evaluation results.
 */
object AggregatedEvaluationResult {
  def apply(results: Traversable[EvaluationResult]): AggregatedEvaluationResult = {
    AggregatedEvaluationResult(
      fMeasure = VariableStatistic(results.map(_.fMeasure)),
      mcc = VariableStatistic(results.map(_.mcc)),
      score = VariableStatistic(results.map(_.score))
    )
  }
}