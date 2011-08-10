package de.fuberlin.wiwiss.silk.learning

import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.evaluation.ReferenceInstances
import java.util.logging.Level
import scala.util.Random

/**
 * Performs multiple cross validation runs and outputs the statistics.
 */
class CrossValidationTask(instances : ReferenceInstances) extends Task[Unit] {
  /** The number of cross validation runs. */
  private val numRuns = 10

  /** The number of splits used for cross-validation. */
  private val numFolds = 2

  /** Don't log progress. */
  logLevel = Level.FINE

  /**
   *
   * Executes all cross validation runs.
   */
  override def execute() {
    //Execute the cross validation runs
    val results = for(run <- 0 until numRuns; result <- crossValidation(run)) yield result
    //Make sure that all runs have the same number of results
    val paddedResults = results.map(r => r.padTo(results.map(_.size).max, r.last))
    //Aggregated the results of each iteration
    val aggregatedResults = for((iterationResults, i) <- paddedResults.transpose.zipWithIndex) yield AggregatedLearningResult(iterationResults, i)

    println(AggregatedLearningResult.format(aggregatedResults, true, true).toLatex)
    println()
    println(AggregatedLearningResult.format(aggregatedResults, false, true).toCsv)
  }

  /**
   * Executes one cross validation run.
   */
  private def crossValidation(run: Int): Iterable[Seq[LearningResult]] = {
    val splits = splitReferenceInstances()

    for((split, index) <- splits.zipWithIndex) yield {
      val learningTask = new LearningTask(split.trainingSet, split.validationSet)

      var results = List[LearningResult]()
      val addResult = (result: LearningResult) => {
        if (result.iterations > results.view.map(_.iterations).headOption.getOrElse(0))
          results ::= result
      }
      learningTask.value.onUpdate(addResult)

      executeSubTask(learningTask, (run.toDouble + index.toDouble / splits.size) / numRuns)

      results.reverse
    }
  }

  /**
   * Splits the reference instances..
   */
  private def splitReferenceInstances(): IndexedSeq[LearningSet] = {
    //Get the positive and negative reference instances
    val posInstances = Random.shuffle(instances.positive.values)
    val negInstances = Random.shuffle(instances.negative.values)

    //Split the reference instances into numFolds samples
    val posSamples = posInstances.grouped((posInstances.size.toDouble / numFolds).ceil.toInt).toStream
    val negSamples = negInstances.grouped((negInstances.size.toDouble / numFolds).ceil.toInt).toStream

    //Generate numFold splits
    val posSplits = (posSamples ++ posSamples).sliding(posSamples.size).take(posSamples.size)
    val negSplits = (negSamples ++ negSamples).sliding(negSamples.size).take(negSamples.size)

    //Generate a learning set from each split
    val splits =
      for((p, n) <- posSplits zip negSplits) yield {
        LearningSet(
          trainingSet = ReferenceInstances.fromInstances(p.tail.flatten, n.tail.flatten),
          validationSet = ReferenceInstances.fromInstances(p.head, n.head)
        )
      }

    splits.toIndexedSeq
  }

  /**
   * A learning set consisting of a training set and a validation set.
   */
  private case class LearningSet(trainingSet: ReferenceInstances, validationSet: ReferenceInstances)
}