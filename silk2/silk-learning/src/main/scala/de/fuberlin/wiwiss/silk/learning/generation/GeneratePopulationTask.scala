package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.util.ParallelMapper
import de.fuberlin.wiwiss.silk.evaluation.LinkageRuleEvaluator
import de.fuberlin.wiwiss.silk.util.task.Task
import util.Random
import de.fuberlin.wiwiss.silk.learning.individual.{LinkageRuleNode, Individual, Population}
import de.fuberlin.wiwiss.silk.learning.{LearningConfiguration, LearningInput}

class GeneratePopulationTask(input: LearningInput, generator: LinkageRuleGenerator, config: LearningConfiguration) extends Task[Population] {

  override def execute(): Population = {
    val individuals = new ParallelMapper(0 until config.parameters.populationSize).map { i =>
      updateStatus(i.toDouble / config.parameters.populationSize);
      generateIndividual()
    }

    Population(individuals)
  }

  private def generateIndividual(): Individual = {
    val linkageRule = generateRule()
    val fitness = LinkageRuleEvaluator(linkageRule.build, input.trainingInstances)

    Individual(linkageRule, fitness)
  }

  private def generateRule() = {
    if(!input.seedLinkageRules.isEmpty && Random.nextDouble() < 0.1) {
      LinkageRuleNode.load(input.seedLinkageRules.toSeq(Random.nextInt(input.seedLinkageRules.size)))
    } else {
      generator()
    }
  }
}
