package de.fuberlin.wiwiss.silk.learning

import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import java.util.logging.Level
import scala.util.Random
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule

/**
 * Performs multiple cross validation runs and outputs the statistics.
 */
class CrossValidationTask(entities : ReferenceEntities, seedLinkageRules: Traversable[LinkageRule] = Traversable.empty) extends Task[Unit] {
  /** The number of cross validation runs. */
  private val numRuns = 2

  /** The number of splits used for cross-validation. */
  private val numFolds = 2

  /** Don't log progress. */
  progressLogLevel = Level.FINE

  private val config = LearningConfiguration.load()

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
    val splits = splitReferenceEntities()

    for((split, index) <- splits.zipWithIndex) yield {
      val learningTask = new LearningTask(split, config)

      var results = List[LearningResult]()
      val addResult = (result: LearningResult) => {
        if (result.iterations > results.view.map(_.iterations).headOption.getOrElse(0))
          results ::= result
      }
      learningTask.value.onUpdate(addResult)

      executeSubTask(learningTask, (run.toDouble + index.toDouble / splits.size) / numRuns)

      //Add the learning result to the list
      results = learningTask.value.get :: results.tail

      results.reverse
    }
  }

  /**
   * Splits the reference entities..
   */
  private def splitReferenceEntities(): IndexedSeq[LearningInput] = {
    //Get the positive and negative reference entities
    val posEntities = Random.shuffle(entities.positive.values)
    val negEntities = Random.shuffle(entities.negative.values)

    //Split the reference entities into numFolds samples
    val posSamples = posEntities.grouped((posEntities.size.toDouble / numFolds).ceil.toInt).toStream
    val negSamples = negEntities.grouped((negEntities.size.toDouble / numFolds).ceil.toInt).toStream

    //Generate numFold splits
    val posSplits = (posSamples ++ posSamples).sliding(posSamples.size).take(posSamples.size)
    val negSplits = (negSamples ++ negSamples).sliding(negSamples.size).take(negSamples.size)

    //Generate a learning set from each split
    val splits =
      for((p, n) <- posSplits zip negSplits) yield {
        LearningInput(
          seedLinkageRules = seedLinkageRules,
          trainingEntities = ReferenceEntities.fromEntities(p.tail.flatten, n.tail.flatten),
          validationEntities = ReferenceEntities.fromEntities(p.head, n.head)
        )
      }

    splits.toIndexedSeq
  }
}