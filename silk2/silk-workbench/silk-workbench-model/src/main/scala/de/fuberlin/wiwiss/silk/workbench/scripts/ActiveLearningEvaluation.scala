package de.fuberlin.wiwiss.silk.workbench.scripts

import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.learning.active.ActiveLearningTask
import de.fuberlin.wiwiss.silk.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import de.fuberlin.wiwiss.silk.workbench.scripts.RunResult.Run
import de.fuberlin.wiwiss.silk.learning.{LearningResult, LearningConfiguration}
import de.fuberlin.wiwiss.silk.entity.Link

object ActiveLearningEvaluation extends EvaluationScript {

  override protected def run() {
    val experiment = Experiment.default
    val datasets = Dataset.fromWorkspace

    val values =
      for(ds <- datasets) yield {
        for(config <- experiment.configurations) yield {
          execute(config, ds)
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

     println(result.toCsv)
  }

  private def execute(config: LearningConfiguration, dataset: Dataset): RunResult = {
    log.info("Running: " + dataset.name)
    val cache = dataset.task.cache
    cache.waitUntilLoaded()
    val task = new ActiveLearningEvaluator(config, dataset)
    task()
  }
}

class ActiveLearningEvaluator(config: LearningConfiguration,
                              ds: Dataset) extends Task[RunResult] {

  val numRuns = 2

  val maxLinks = 30

  protected override def execute() = {
    //Execute the active learning runs
    val results = for(run <- 0 until numRuns) yield runActiveLearning(run)

    //Print aggregated results
    val aggregatedResults = for((iterationResults, i) <- results.transpose.zipWithIndex) yield AggregatedLearningResult(iterationResults, i)

    println(AggregatedLearningResult.format(aggregatedResults, includeStandardDeviation = true, includeComplexity = false).toLatex)
    println()
    println(AggregatedLearningResult.format(aggregatedResults, includeStandardDeviation = false, includeComplexity = false).toCsv)

    RunResult(results.map(Run(_)))
  }

  private def runActiveLearning(run: Int): Seq[LearningResult] = {
    logger.info("Active Learning run " + run)

    var referenceEntities = ReferenceEntities()
    val validationEntities = ds.task.cache.entities

    val positiveValLinks = for((link, entityPair) <- validationEntities.positive) yield link.update(entities = Some(entityPair))
    val negativeValLinks = for((link, entityPair) <- validationEntities.negative) yield link.update(entities = Some(entityPair))
    var pool: Traversable[Link] = positiveValLinks ++ negativeValLinks
    var population = Population.empty
    val startTime = System.currentTimeMillis()

    //Holds the validation result from each iteration
    var learningResults = List[LearningResult]()

    for(i <- 0 to maxLinks) {
      val task =
        new ActiveLearningTask(
          config = config,
          sources = ds.sources,
          linkSpec = ds.task.linkSpec,
          paths = ds.task.cache.entityDescs.map(_.paths),
          referenceEntities = referenceEntities,
          pool = pool,
          population = population
        )

      task()

      pool = task.pool
      population = task.population

      //Evaluate performance of learned linkage rule
      val rule = population.bestIndividual.node.build
      val trainScores = LinkageRuleEvaluator(rule, referenceEntities)
      val valScores = LinkageRuleEvaluator(rule, validationEntities)
      val learningResult =
        LearningResult(
          iterations = i,
          time = System.currentTimeMillis() - startTime,
          population = population,
          trainingResult =  trainScores,
          validationResult = valScores,
          status = LearningResult.NotStarted
        )

      println(i + " - " + trainScores)
      println(i + " - " + valScores)
      learningResults ::= learningResult
//      if(valScores.fMeasure > 0.999) {
//        return learningResults.reverse
//      }

      //Evaluate new link
      val link = task.links.head
      if(validationEntities.positive.contains(link)) {
        println(link + " added to positive")
        referenceEntities = referenceEntities.withPositive(link.entities.get)
      }
      else {
        println(link + " added to negative")
        referenceEntities = referenceEntities.withNegative(link.entities.get)
      }
    }

    learningResults.reverse
  }
}