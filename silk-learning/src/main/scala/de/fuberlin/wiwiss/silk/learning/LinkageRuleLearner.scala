package de.fuberlin.wiwiss.silk.learning

import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.learning.LinkageRuleLearner.Result
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.rule.LinkageRule
import de.fuberlin.wiwiss.silk.runtime.activity.Activity

trait LinkageRuleLearner {

  // TODO change ReferenceEntities to ReferenceLinks
  def learn(trainingLinks: ReferenceEntities, seeds: Traversable[LinkageRule]): Activity[Result]

}

object LinkageRuleLearner {
  
  case class Result(population: Population, iterations: Int, message: String)
}