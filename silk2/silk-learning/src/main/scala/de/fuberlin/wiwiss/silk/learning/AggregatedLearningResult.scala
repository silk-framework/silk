package de.fuberlin.wiwiss.silk.learning

import de.fuberlin.wiwiss.silk.evaluation.{AggregatedEvaluationResult, VariableStatistic}

/**
 * The aggregated result of multiple learning runs.
 */
case class AggregatedLearningResult(iterations: Int,
                                    time: VariableStatistic,
                                    trainingResult: AggregatedEvaluationResult,
                                    validationResult: AggregatedEvaluationResult)

/**
 * Aggregates the results of multiple learning runs.
 */
object AggregatedLearningResult {
  def apply(results: Traversable[LearningResult]): AggregatedLearningResult = {
    require(results.tail.forall(_.iterations == results.head.iterations), "All results must be from the same iteration")

    AggregatedLearningResult(
      iterations = results.head.iterations,
      time = VariableStatistic(results.map(_.time.toDouble)),
      trainingResult = AggregatedEvaluationResult(results.map(_.population.bestIndividual.fitness)),
      validationResult = AggregatedEvaluationResult(results.map(_.validationResult))
    )
  }
}