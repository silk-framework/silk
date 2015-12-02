package org.silkframework.workspace.scripts

import java.util.logging.Logger
import org.silkframework.entity.Link
import org.silkframework.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import org.silkframework.learning.active.{UnlabeledLinkPool, ActiveLearningState, ActiveLearning}
import org.silkframework.learning.individual.Population
import org.silkframework.learning.{LearningConfiguration, LearningResult}
import org.silkframework.runtime.activity.{ActivityContext, Activity}
import org.silkframework.util.DPair
import org.silkframework.workspace.activity.linking.{LinkingPathsCache, ReferenceEntitiesCache}
import org.silkframework.workspace.scripts.RunResult.Run

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
    val cache = dataset.task.activity[ReferenceEntitiesCache]
    while(cache.status.isRunning)
      Thread.sleep(200)
    Activity(new ActiveLearningEvaluator(config, dataset)).startBlockingAndGetValue()
  }
}

class ActiveLearningEvaluator(config: LearningConfiguration,
                              ds: Data) extends Activity[RunResult] {

  val numRuns = 1

  val maxLinks = 1

  val maxPosRefLinks = 100

  val maxNegRefLinks = 3000

  override def run(context: ActivityContext[RunResult]): Unit = {
    //Execute the active learning runs
    val results = for(run <- 1 to numRuns) yield runActiveLearning(run)

    //Print aggregated results
    val aggregatedResults = for((iterationResults, i) <- results.transpose.zipWithIndex) yield AggregatedLearningResult(iterationResults, i)

    println("Results for experiment " + config.name + " on data set " + ds.name)
    println(AggregatedLearningResult.format(aggregatedResults, includeStandardDeviation = true, includeComplexity = false).toLatex)
    println()
    println(AggregatedLearningResult.format(aggregatedResults, includeStandardDeviation = false, includeComplexity = false).toCsv)

    context.value.update(RunResult(results.map(Run)))
  }

  private def runActiveLearning(run: Int): Seq[LearningResult] = {
    Logger.getLogger(getClass.getName).info("Experiment " + config.name + " on data set " + ds.name +  ": run " + run )

    var referenceEntities = ReferenceEntities()
    val validationEntities = ds.task.activity[ReferenceEntitiesCache].value

    val sourceEntities =  validationEntities.positive.values.map(_.source)
    val targetEntities =  validationEntities.positive.values.map(_.target)
    val positiveValLinks = for((link, entityPair) <- validationEntities.positive) yield link.update(entities = Some(entityPair))
    val negativeValLinks = for(s <- sourceEntities; t <- targetEntities) yield new Link(s.uri, t.uri, None, Some(DPair(s, t)))

    var pool = UnlabeledLinkPool.empty //positiveValLinks.take(maxPosRefLinks) ++ Random.shuffle(negativeValLinks).take(maxNegRefLinks)
    var population = Population.empty
    val startTime = System.currentTimeMillis()

    //Holds the validation result from each iteration
    var learningResults = List[LearningResult]()

    for(i <- 0 to maxLinks) {
      val activity =
        new ActiveLearning(
          config = config,
          datasets = ds.sources,
          linkSpec = ds.task.data,
          paths = ds.task.activity[LinkingPathsCache].value.map(_.paths),
          referenceEntities = referenceEntities,
          initialState = ActiveLearningState(pool, population, Seq.empty)
        )

      val result = Activity(activity).startBlockingAndGetValue()
      pool = result.pool
      population = result.population

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
      val link = result.links.head
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