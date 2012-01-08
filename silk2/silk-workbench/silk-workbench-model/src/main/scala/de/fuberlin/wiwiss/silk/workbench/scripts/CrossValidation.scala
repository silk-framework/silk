/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workbench.scripts

import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import java.util.logging.Level
import scala.util.Random
import de.fuberlin.wiwiss.silk.learning._
import de.fuberlin.wiwiss.silk.workbench.scripts.RunResult.Run

object CrossValidation extends EvaluationScript {

  override protected def run() {
    runExperiment()
    println("Evaluation finished")
  }

  protected def runDefault() {
    execute(
      dataset = Dataset.fromWorkspace.head,
      config = LearningConfiguration()
    )
  }

  protected def runExperiment() {
    val experiment = Experiment.seeding
    val metrics = PerformanceMetric.all
    val datasets = Dataset.fromWorkspace
    
    val values =
      for(config <- experiment.configurations) yield {
        for(ds <- datasets) yield {
          execute(ds, config)
        }
      }
    
    val result = MultipleTables.build(metrics, values, datasets.map(_.name), experiment.configurations.map(_.name))

    println(result.toCsv)
  }
  
  private def execute(dataset: Dataset, config: LearningConfiguration) = {
    val cache = dataset.task.cache
    cache.waitUntilLoaded()
    val task = new CrossValidation(cache.entities, config)
    task()
  }
}

/**
 * Performs multiple cross validation runs and outputs the statistics.
 */
class CrossValidation(entities : ReferenceEntities, config: LearningConfiguration) extends Task[RunResult] {
  require(entities.isDefined, "Reference Entities are required")
  
  /** The number of cross validation runs. */
  private val numRuns = 3

  /** The number of splits used for cross-validation. */
  private val numFolds = 2

  /** Don't log progress. */
  progressLogLevel = Level.FINE

  /**
   * Executes all cross validation runs.
   */
  override def execute() = {
    //Execute the cross validation runs
    val results = for(run <- 0 until numRuns; result <- crossValidation(run)) yield result
    //Make sure that all runs have the same number of results
    val paddedResults = results.map(r => r.padTo(results.map(_.size).max, r.last))
    
    RunResult(paddedResults.map(r => Run(r.map(_.trainingResult.fMeasure))))

    //Aggregated the results of each iteration
//    val aggregatedResults = for((iterationResults, i) <- paddedResults.transpose.zipWithIndex) yield AggregatedLearningResult(iterationResults, i)
//
//    println(AggregatedLearningResult.format(aggregatedResults, includeStandardDeviation = true, includeComplexity = true).toLatex)
//    println()
//    println(AggregatedLearningResult.format(aggregatedResults, includeStandardDeviation = false, includeComplexity = true).toCsv)
  }

  /**
   * Executes one cross validation run.
   */
  private def crossValidation(run: Int): Seq[Seq[LearningResult]] = {
    logger.info("Cross validation run " + run)
    
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
          seedLinkageRules = Seq.empty,
          trainingEntities = ReferenceEntities.fromEntities(p.tail.flatten, n.tail.flatten),
          validationEntities = ReferenceEntities.fromEntities(p.head, n.head)
        )
      }

    splits.toIndexedSeq
  }
}