package de.fuberlin.wiwiss.silk.workbench.scripts

import de.fuberlin.wiwiss.silk.learning.LearningConfiguration
import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.learning.active.ActiveLearningTask
import de.fuberlin.wiwiss.silk.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration.Parameters
import de.fuberlin.wiwiss.silk.workbench.scripts.ExperimentResult.{Table, Row}
import util.Random
import de.fuberlin.wiwiss.silk.plugins.Plugins
import de.fuberlin.wiwiss.silk.plugins.jena.JenaPlugins

object ActiveLearningEvaluation extends App {

  Plugins.register()
  JenaPlugins.register()

  val labels = "Mean iterations for 90%" :: "Mean iterations for 95%" :: "Mean iterations for 100%" :: Nil

  val configurations =
    Seq(
      "test1" -> LearningConfiguration.load().copy(params = Parameters(seed = false)),
      "test2" -> LearningConfiguration.load().copy(params = Parameters(seed = false))
    )
  
  val experiments = Experiment.experiments

  val values =
    for(ex <- experiments) yield {
      for(config <- configurations.map(_._2)) yield {
        new ActiveLearningEvaluator(config, ex).apply()
      }
    }

  val result =
    ExperimentResult(
      for((label, index) <- labels.zipWithIndex) yield {
        val rows =
          for((c,v) <- configurations.map(_._1) zip values) yield {
            Row(c, v.map(_(index)))
          }
        Table(label, experiments.map(_.name), rows)
      }
    )

   println(result.toCsv)

}

class ActiveLearningEvaluator(config: LearningConfiguration,
                              experiment: Experiment) extends Task[Seq[Double]] {

  val numRuns = 10

  protected override def execute() = {
    //Execute runs
    //val results = List.fill(numRuns)(run())

    //Random.nextDouble :: Random.nextDouble :: Random.nextDouble :: Nil
    0.1 :: 0.2 :: 0.3 :: Nil
      //values = RunStatistic.meanIterations(results, 0.9) ::
      //         RunStatistic.meanIterations(results, 0.95) ::
      //         RunStatistic.meanIterations(results, 0.999) :: Nil
    
  }

  private def run(): RunStatistic = {
    var referenceEntities = ReferenceEntities()
    val validationEntities = experiment.task.cache.entities

    val positiveValLinks = for((link, entityPair) <- validationEntities.positive) yield link.update(entities = Some(entityPair))
    val negativeValLinks = for((link, entityPair) <- validationEntities.negative) yield link.update(entities = Some(entityPair))
    var pool: Traversable[Link] = positiveValLinks ++ negativeValLinks
    var population = Population()

    //Holds the validation result from each iteration
    var scores = List[Double]()

    for(i <- 0 to 1000) {
      val task =
        new ActiveLearningTask(
          config = config,
          sources = experiment.sources,
          linkSpec = experiment.task.linkSpec,
          paths = experiment.task.cache.entityDescs.map(_.paths),
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
      println(i + " - " + trainScores)
      println(i + " - " + valScores)
      scores ::= valScores.fMeasure
      if(valScores.fMeasure > 0.999) {
        return RunStatistic(scores.reverse)
      }

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

    RunStatistic(scores.reverse)
  }

  private case class RunStatistic(results: List[Double]) {
    def format() = {
      "F-measure\n" + results.mkString("\n")
    }

    /**
     * Compute the number of iterations needed to reach a specific F-measure.
     */
    def iterations(fMeasure: Double): Int = {
      results.indexWhere(_ >= fMeasure) match {
        case -1 => throw new IllegalArgumentException("Target F-measure " + fMeasure + " never reached.")
        case i => i
      }
    }
  }

  private object RunStatistic {
    def merge(statistics: List[RunStatistic]) = {
      val maxIterations = statistics.map(_.results.size).max
      val fMeasures = statistics.map(_.results.padTo(maxIterations, 1.0))
      val meanfMeasures = fMeasures.transpose.map(d => d.sum / d.size)
      RunStatistic(meanfMeasures)
    }

    /**
     * Compute the average number of iterations needed to reach a specific F-measure.
     */
    def meanIterations(statistics: List[RunStatistic], fMeasure: Double): Double = {
      statistics.map(_.iterations(fMeasure)).sum.toDouble / statistics.size
    }
  }
}