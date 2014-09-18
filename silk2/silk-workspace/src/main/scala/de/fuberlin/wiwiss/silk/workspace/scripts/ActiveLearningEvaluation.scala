package de.fuberlin.wiwiss.silk.workspace.scripts

import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import de.fuberlin.wiwiss.silk.learning.active.ActiveLearningTask
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.learning.{LearningConfiguration, LearningResult}
import de.fuberlin.wiwiss.silk.runtime.task.Task
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.workspace.scripts.RunResult.Run

object ActiveLearningEvaluation extends EvaluationScript {

  override protected def run() {
    val experiment = Experiment.default
    val datasets = Data.fromWorkspace

    val values =
      for(dataset <- datasets) yield {
        for(config <- experiment.configurations) yield {
          execute(config, dataset)
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
     println(result.toCsv)
  }

  private def execute(config: LearningConfiguration, dataset: Data): RunResult = {
    val cache = dataset.task.cache
    cache.waitUntilLoaded()
    val task = new ActiveLearningEvaluator(config, dataset)
    task()
  }
}

class ActiveLearningEvaluator(config: LearningConfiguration,
                              ds: Data) extends Task[RunResult] {

  val numRuns = 1

  val maxLinks = 1

  val maxPosRefLinks = 100

  val maxNegRefLinks = 3000

  protected override def execute() = {
    //Execute the active learning runs
    val results = for(run <- 1 to numRuns) yield runActiveLearning(run)

    //Print aggregated results
    val aggregatedResults = for((iterationResults, i) <- results.transpose.zipWithIndex) yield AggregatedLearningResult(iterationResults, i)

    println("Results for experiment " + config.name + " on data set " + ds.name)
    println(AggregatedLearningResult.format(aggregatedResults, includeStandardDeviation = true, includeComplexity = false).toLatex)
    println()
    println(AggregatedLearningResult.format(aggregatedResults, includeStandardDeviation = false, includeComplexity = false).toCsv)

    RunResult(results.map(Run(_)))
  }

  private def runActiveLearning(run: Int): Seq[LearningResult] = {
    logger.info("Experiment " + config.name + " on data set " + ds.name +  ": run " + run )

    var referenceEntities = ReferenceEntities()
    val validationEntities = ds.task.cache.entities

    val sourceEntities =  validationEntities.positive.values.map(_.source)
    val targetEntities =  validationEntities.positive.values.map(_.target)
    val positiveValLinks = for((link, entityPair) <- validationEntities.positive) yield link.update(entities = Some(entityPair))
    val negativeValLinks = for(s <- sourceEntities; t <- targetEntities) yield new Link(s.uri, t.uri, None, Some(DPair(s, t)))

    var pool: Traversable[Link] = Nil//positiveValLinks.take(maxPosRefLinks) ++ Random.shuffle(negativeValLinks).take(maxNegRefLinks)
    var population = Population.empty
    val startTime = System.currentTimeMillis()

    //Holds the validation result from each iteration
    var learningResults = List[LearningResult]()

    for(i <- 0 to maxLinks) {
      val task =
        new ActiveLearningTask(
          config = config,
          datasets = ds.sources,
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
          status = ""
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