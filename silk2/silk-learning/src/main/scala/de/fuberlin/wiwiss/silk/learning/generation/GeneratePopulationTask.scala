package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.util.ParallelMapper
import de.fuberlin.wiwiss.silk.evaluation.LinkConditionEvaluator
import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.learning.LearningInput
import util.Random
import de.fuberlin.wiwiss.silk.learning.individual.{LinkConditionNode, Individual, Population}

class GeneratePopulationTask(input: LearningInput, generator: LinkConditionGenerator) extends Task[Population] {
  val populationSize = 500

  override def execute(): Population = {
    val individuals = new ParallelMapper(0 until populationSize).map { i =>
      updateStatus(i.toDouble / populationSize);
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
