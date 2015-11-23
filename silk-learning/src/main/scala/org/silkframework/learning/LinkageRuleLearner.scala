package org.silkframework.learning

import org.silkframework.learning.LinkageRuleLearner.Result
import org.silkframework.learning.individual.Population
import org.silkframework.evaluation.ReferenceEntities
import org.silkframework.rule.LinkageRule
import org.silkframework.runtime.activity.Activity

trait LinkageRuleLearner {

  // TODO change ReferenceEntities to ReferenceLinks
  def learn(trainingLinks: ReferenceEntities, seeds: Traversable[LinkageRule]): Activity[Result]

}

object LinkageRuleLearner {
  
  case class Result(population: Population, iterations: Int, message: String)
}