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

package de.fuberlin.wiwiss.silk.workspace.scripts

import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.learning._
import de.fuberlin.wiwiss.silk.runtime.activity.{Activity, ActivityContext}
import de.fuberlin.wiwiss.silk.workspace.modules.linking.{ReferenceEntitiesCache}
import de.fuberlin.wiwiss.silk.workspace.scripts.RunResult.Run

import scala.util.Random

object CrossValidation extends EvaluationScript {

  override protected def run() {
    runExperiment()
    println("Evaluation finished")
  }

  protected def runExperiment() {
    val experiment = Experiment.default
    val datasets = Data.fromWorkspace
    
    val values =
      for(ds <- datasets) yield {
        for(config <- experiment.configurations) yield {
          execute(ds, config)
        }
      }

    val result =
      MultipleTables.build(
        name = experiment.name,
        metrics = experiment.metrics,
        header = experiment.configurations.map(_.name),
        rowLabels = datasets.map(_.name),
        values = values
      )

    println(result.toLatex)
    println(result.transpose.toLatex)
  }
  
  private def execute(dataset: Data, config: LearningConfiguration): RunResult = {
    log.info("Running: " + dataset.name)
    val cache = dataset.task.activity[ReferenceEntitiesCache]
    while(cache.status().isRunning)
      Thread.sleep(200)
    Activity(new CrossValidation(cache.value(), config)).startBlocking()
  }
}

/**
 * Performs multiple cross validation runs and outputs the statistics.
 */
class CrossValidation(entities : ReferenceEntities, config: LearningConfiguration) extends Activity[RunResult] {
  require(entities.isDefined, "Reference Entities are required")
  
  /** The number of cross validation runs. */
  private val numRuns = 10

  /** The number of splits used for cross-validation. */
  private val numFolds = 2

  /**
   * Executes all cross validation runs.
   */
  override def run(context: ActivityContext[RunResult]) = {
    //Execute the cross validation runs
    val results = for(run <- 0 until numRuns; result <- crossValidation(run, context)) yield result
    //Make sure that all runs have the same number of results
    val paddedResults = results.map(r => r.padTo(config.params.maxIterations + 1, r.last))

    //Print aggregated results
    val aggregatedResults = for((iterationResults, i) <- paddedResults.transpose.zipWithIndex) yield AggregatedLearningResult(iterationResults, i)

    println(AggregatedLearningResult.format(aggregatedResults, includeStandardDeviation = true, includeComplexity = false).toLatex)
    println()
    println(AggregatedLearningResult.format(aggregatedResults, includeStandardDeviation = false, includeComplexity = false).toCsv)

    RunResult(paddedResults.map(Run(_)))
  }

  /**
   * Executes one cross validation run.
   */
  private def crossValidation(run: Int, context: ActivityContext[RunResult]): Seq[Seq[LearningResult]] = {
    context.log.info("Cross validation run " + run)
    
    val splits = splitReferenceEntities()

    for((split, index) <- splits.zipWithIndex) yield {
      val learningActivity = new LearningActivity(split, config)

      var results = List[LearningResult]()
      val addResult = (result: LearningResult) => {
        if (result.iterations > results.view.map(_.iterations).headOption.getOrElse(0))
          results ::= result
      }
      results = context.executeBlocking(learningActivity, index.toDouble / splits.size / numRuns, addResult) :: results.tail

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