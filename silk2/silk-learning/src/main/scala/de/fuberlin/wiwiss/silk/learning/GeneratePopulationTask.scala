package de.fuberlin.wiwiss.silk.learning

import generation.RandomGenerator
import de.fuberlin.wiwiss.silk.util.ParallelMapper
import de.fuberlin.wiwiss.silk.evaluation.{ReferenceInstances, LinkConditionEvaluator}
import de.fuberlin.wiwiss.silk.util.task.Task
import individual.Individual

class GeneratePopulationTask(instances : ReferenceInstances, config : LearningConfiguration) extends Task[Population]
{
  val populationSize = 500

  override def execute() : Population =
  {
    val individuals = new ParallelMapper(0 until populationSize).map{ i => updateStatus(i.toDouble / populationSize); generate() }

    Population(individuals)
  }

  private def generate() : Individual =
  {
    val linkCondition = RandomGenerator(config.generation)
    val fitness = LinkConditionEvaluator(linkCondition.build, instances)

    Individual(linkCondition, fitness)
  }
}
