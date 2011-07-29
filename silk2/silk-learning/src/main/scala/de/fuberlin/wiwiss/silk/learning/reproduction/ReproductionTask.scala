package de.fuberlin.wiwiss.silk.learning.reproduction

import util.Random
import de.fuberlin.wiwiss.silk.util.{ParallelMapper, SourceTargetPair}
import de.fuberlin.wiwiss.silk.evaluation.{ReferenceInstances, LinkConditionEvaluator}
import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration
import de.fuberlin.wiwiss.silk.learning.individual.{Individual, Population}
import de.fuberlin.wiwiss.silk.learning.generation.RandomGenerator

class ReproductionTask(population : Population, instances : ReferenceInstances, config : LearningConfiguration) extends Task[Population]
{
  private val mutationProbability = 0.25

  private val elitismCount = 3

  private val tournamentSize = 5

  private val keepHistory = false

  val crossoverOperators = config.crossover.operators.toIndexedSeq

  private val individuals = population.individuals.toArray

  override def execute() : Population =
  {
    val elite = individuals.sortBy(-_.fitness.score).take(elitismCount)

    //Number of individuals to be generated
    val count = individuals.size - elitismCount

    val offspring = new ParallelMapper(0 until count).map{ i => updateStatus(i.toDouble / count); reproduce() }

    Population(elite ++ offspring)
  }

  private def reproduce() : Individual =
  {
    //Choose a random crossover operator
    val operator = crossoverOperators(Random.nextInt(crossoverOperators.size))

    //Define the two crossover individuals: In case of mutation, we do a crossover with a new random node
    val sourceIndividual = select()
    val targetLinkCondition = if(Random.nextDouble < mutationProbability) RandomGenerator(config.generation) else select().node

    operator(SourceTargetPair(sourceIndividual.node, targetLinkCondition)) match
    {
      case Some(node) =>
      {
        val startTime = System.currentTimeMillis()

        val fitness = LinkConditionEvaluator(node.build, instances)

        val time = System.currentTimeMillis() - startTime

        Individual(node, fitness)//, if(keepHistory) Some(Individual.Base(operator, sourceIndividual)) else None, time)
      }
      case None =>
      {
        //No compatible pairs for this operator found => return unmodified node
        sourceIndividual
      }
    }
  }

  private def select() : Individual =
  {
    val tournamentNodes = List.fill(tournamentSize)(individuals(Random.nextInt(individuals.size)))

    tournamentNodes.reduceLeft((n1, n2) => if(n1.fitness.score > n2.fitness.score) n1 else n2)
  }
}