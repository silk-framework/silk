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
  /**
   * Aggregates multiple learning results.
   */
  def apply(results: Traversable[LearningResult], iteration: Int): AggregatedLearningResult = {
    AggregatedLearningResult(
      iterations = iteration,
      time = VariableStatistic(results.map(_.time.toDouble)),
      trainingResult = AggregatedEvaluationResult(results.map(_.population.bestIndividual.fitness)),
      validationResult = AggregatedEvaluationResult(results.map(_.validationResult))
    )
  }

  /**
   * Formats a sequence of aggregated learning results.
   */
  def format(results: Seq[AggregatedLearningResult], includeStandardDeviation: Boolean = true): ResultTable = {
    new Formatter(includeStandardDeviation).apply(results)
  }

  private class Formatter(includeStandardDeviation: Boolean) {
    def apply(results: Seq[AggregatedLearningResult]): ResultTable = {
      ResultTable(
        header = "Iter." :: ("Time in s" :: "Train. F1" :: "Train. MCC" :: "Val. F1" :: "Val. MCC" :: Nil).map(withDerivation),
        values = results.zipWithIndex.map(row _ tupled)
      )
    }

    private def withDerivation(name: String) = if(includeStandardDeviation) name + " (\\(\\sigma\\))" else name

    private def row(res: AggregatedLearningResult, run: Int): Seq[String] = {
      res.iterations.toString :: format(res.time.map(_ / 1000.0), 1) ::
      format(res.trainingResult.fMeasure, 3) :: format(res.trainingResult.mcc, 3) ::
      format(res.validationResult.fMeasure, 3) :: format(res.validationResult.mcc, 3) :: Nil
    }

    private def format(statistics: VariableStatistic, precision: Int = 3): String = {
      val mean = ("%." + precision + "f").format(statistics.mean)

      if(includeStandardDeviation)
        mean + " (" + ("%." + precision + "f").format(statistics.standardDeviation) + ")"
      else
        mean
    }
  }
}