package de.fuberlin.wiwiss.silk.learning

import de.fuberlin.wiwiss.silk.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.runtime.task.{Task, ValueTask}
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.runtime.task
import de.fuberlin.wiwiss.silk.learning.LinkageRuleLearner.Result
import de.fuberlin.wiwiss.silk.learning.reproduction.ReproductionTask

trait LinkageRuleLearner {

  // TODO change ReferenceEntities to ReferenceLinks
  def learn(trainingLinks: ReferenceEntities, seeds: Traversable[LinkageRule]): ValueTask[Result]

}

object LinkageRuleLearner {
  
  case class Result(population: Population, iterations: Int, message: String)
}