package de.fuberlin.wiwiss.silk.workbench.scripts

import de.fuberlin.wiwiss.silk.learning.LearningConfiguration
import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.learning.active.ActiveLearningTask
import de.fuberlin.wiwiss.silk.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration.Parameters
import de.fuberlin.wiwiss.silk.plugins.Plugins
import de.fuberlin.wiwiss.silk.plugins.jena.JenaPlugins
import de.fuberlin.wiwiss.silk.workbench.scripts.RunResult.Run

object ActiveLearningEvaluation extends App {

  Plugins.register()
  JenaPlugins.register()

  val labels = "Mean iterations for 90%" :: "Mean iterations for 95%" :: "Mean iterations for 100%" :: Nil

  val configurations =
    Seq(
      LearningConfiguration("test1", params = Parameters(seed = false)),
      LearningConfiguration("test2", params = Parameters(seed = false))
    )
  
  val datasets = Dataset.fromWorkspace

  val values =
    for(ds <- datasets) yield {
      for(config <- configurations) yield {
        new ActiveLearningEvaluator(config, ds).apply()
      }
    }

  val result =
    MultipleTables(
      for((label, index) <- labels.zipWithIndex) yield {
        val rows = for(v <- values) yield v.map(_(index))
        Table(label, datasets.map(_.name), configurations.map(_.name), rows)
      }
    )

   println(result.toCsv)

}

class ActiveLearningEvaluator(config: LearningConfiguration,
                              ds: Dataset) extends Task[Seq[Double]] {

  val numRuns = 10

  protected override def execute() = {
    //Execute runs
    //val results = List.fill(numRuns)(run())

    //Random.nextDouble :: Random.nextDouble :: Random.nextDouble :: Nil
    0.1 :: 0.2 :: 0.3 :: Nil
      //values = RunResult.meanIterations(results, 0.9) ::
      //         RunResult.meanIterations(results, 0.95) ::
      //         RunResult.meanIterations(results, 0.999) :: Nil
    
  }

  private def run(): Run = {
    var referenceEntities = ReferenceEntities()
    val validationEntities = ds.task.cache.entities

    val positiveValLinks = for((link, entityPair) <- validationEntities.positive) yield link.update(entities = Some(entityPair))
    val negativeValLinks = for((link, entityPair) <- validationEntities.negative) yield link.update(entities = Some(entityPair))
    var pool: Traversable[Link] = positiveValLinks ++ negativeValLinks
    var population = Population.empty

    //Holds the validation result from each iteration
    var scores = List[Double]()

    for(i <- 0 to 1000) {
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
      println(i + " - " + trainScores)
      println(i + " - " + valScores)
      scores ::= valScores.fMeasure
      if(valScores.fMeasure > 0.999) {
        //TODO
        //return Run(scores.reverse)
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

    //TODO Run(scores.reverse)
    null
  }
}