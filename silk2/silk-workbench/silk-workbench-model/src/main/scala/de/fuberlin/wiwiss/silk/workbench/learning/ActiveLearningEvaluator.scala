package de.fuberlin.wiwiss.silk.workbench.learning

import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.learning.active.ActiveLearningTask
import de.fuberlin.wiwiss.silk.learning.active.ActiveLearningTask._
import de.fuberlin.wiwiss.silk.workbench.workspace.User._
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.evaluation.{LinkageRuleEvaluator, ReferenceEntities, ReferenceLinks}
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.entity.{Path, Link}
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration

class ActiveLearningEvaluator(config: LearningConfiguration,
                              sources: Traversable[Source],
                              linkSpec: LinkSpecification,
                              paths: DPair[Seq[Path]],
                              validationEntities: ReferenceEntities) extends Task[Unit] {

  override def execute() {
    var referenceEntities = ReferenceEntities()
    var pool = Traversable[Link]()
    var population = CurrentPopulation()

    for(i <- 0 to 20) {
      val task =
        new ActiveLearningTask(
          config = config,
          sources = sources,
          linkSpec = linkSpec,
          paths = paths,
          referenceEntities = referenceEntities,
          pool = pool,
          population = population
        )

      task()

      pool = task.pool
      population = task.population

      //Evaluate performance of learned linkage rule
      val scores = LinkageRuleEvaluator(task.population.bestIndividual.node.build, validationEntities)
      println(i + " - " + scores)
      if(scores.fMeasure > 0.999) return

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
      //Remove evaluated link from pool
      pool = pool.filterNot(_ == link)
    }
  }


}