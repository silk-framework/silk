package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.util.ParallelMapper
import de.fuberlin.wiwiss.silk.evaluation.{ReferenceInstances, LinkConditionEvaluator}
import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration
import de.fuberlin.wiwiss.silk.learning.individual.{Individual, Population}

class GeneratePopulationTask(instances : ReferenceInstances, generator: IndividualGenerator) extends Task[Population]
{
  val populationSize = 500

  override def execute() : Population =
  {
    val individuals = new ParallelMapper(0 until populationSize).map{ i =>
      updateStatus(i.toDouble / populationSize);
      generateIndividual()
    }

    Population(individuals)
  }

  private def generateIndividual() : Individual =
  {
    val linkCondition = generator()
    val fitness = LinkConditionEvaluator(linkCondition.build, instances)

    Individual(linkCondition, fitness)
  }
}
