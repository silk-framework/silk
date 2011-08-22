package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.util.ParallelMapper
import de.fuberlin.wiwiss.silk.evaluation.LinkConditionEvaluator
import de.fuberlin.wiwiss.silk.util.task.Task
import util.Random
import de.fuberlin.wiwiss.silk.learning.individual.{LinkConditionNode, Individual, Population}
import de.fuberlin.wiwiss.silk.learning.{LearningConfiguration, LearningInput}

class GeneratePopulationTask(input: LearningInput, generator: LinkConditionGenerator, config: LearningConfiguration) extends Task[Population] {

  override def execute(): Population = {
    val individuals = new ParallelMapper(0 until config.parameters.populationSize).map { i =>
      updateStatus(i.toDouble / config.parameters.populationSize);
      generateIndividual()
    }

    Population(individuals)
  }

  private def generateIndividual(): Individual = {
    val linkCondition = generateCondition()
    val fitness = LinkConditionEvaluator(linkCondition.build, input.trainingInstances)

    Individual(linkCondition, fitness)
  }

  private def generateCondition() = {
    if(!input.seedConditions.isEmpty && Random.nextDouble() < 0.1) {
      LinkConditionNode.load(input.seedConditions.toSeq(Random.nextInt(input.seedConditions.size)))
    } else {
      generator()
    }
  }
}
