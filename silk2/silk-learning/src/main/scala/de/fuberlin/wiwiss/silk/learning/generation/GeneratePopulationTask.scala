package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.util.ParallelMapper
import de.fuberlin.wiwiss.silk.evaluation.LinkageRuleEvaluator
import de.fuberlin.wiwiss.silk.util.task.Task
import util.Random
import de.fuberlin.wiwiss.silk.learning.individual.{LinkageRuleNode, Individual, Population}
import de.fuberlin.wiwiss.silk.learning.{LearningConfiguration, LearningInput}
import de.fuberlin.wiwiss.silk.linkagerule.{input, LinkageRule}

/**
 * Generates a new population of linkage rules.
 */
class GeneratePopulationTask(seedLinkageRules: Traversable[LinkageRule], generator: LinkageRuleGenerator, config: LearningConfiguration) extends Task[Population] {

  override def execute(): Population = {
    val individuals = new ParallelMapper(0 until config.parameters.populationSize).map { i =>
      updateStatus(i.toDouble / config.parameters.populationSize);
      generateIndividual()
    }

    Population(individuals)
  }

  private def generateIndividual(): Individual = {
    val linkageRule = generateRule()
    Individual(linkageRule, 0.0)
  }

  private def generateRule() = {
    if(!seedLinkageRules.isEmpty && Random.nextDouble() < 0.1)
      LinkageRuleNode.load(seedLinkageRules.toSeq(Random.nextInt(seedLinkageRules.size)))
    else
      generator()
  }
}
