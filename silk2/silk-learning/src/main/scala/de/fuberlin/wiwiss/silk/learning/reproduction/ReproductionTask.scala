package de.fuberlin.wiwiss.silk.learning.reproduction

import util.Random
import de.fuberlin.wiwiss.silk.util.{ParallelMapper, SourceTargetPair}
import de.fuberlin.wiwiss.silk.evaluation.{ReferenceEntities, LinkageRuleEvaluator}
import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.learning.individual.{Individual, Population}
import de.fuberlin.wiwiss.silk.learning.generation.LinkageRuleGenerator
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration

class ReproductionTask(population: Population, referenceEntities: ReferenceEntities, generator: LinkageRuleGenerator, config: LearningConfiguration) extends Task[Population] {

  /**
   * The operators which will be employed for crossover.
   */
  private val crossoverOperators = {
    var operators = List[CrossoverOperator]()

    //We always learn thresholds and weights
    operators ::= ThresholdCrossover()
    operators ::= WeightCrossover()

    if(config.components.transformations) {
      operators ::= TransformationCrossover()
    }

    if(config.components.aggregations) {
      operators ::= AggregationOperatorsCrossover()
      operators ::= AggregationStrategyCrossover()
      operators ::= OperatorCrossover()
    }

    operators
  }

  private val individuals = population.individuals.toArray

  override def execute(): Population = {
    val elite = individuals.sortBy(-_.fitness.score).take(config.reproduction.elitismCount)

    //Number of individuals to be generated
    val count = individuals.size - config.reproduction.elitismCount

    val offspring = new ParallelMapper(0 until count).map {
      i => updateStatus(i.toDouble / count); reproduce()
    }

    Population(elite ++ offspring)
  }

  private def reproduce(): Individual = {
    //Choose a random crossover operator
    val operator = crossoverOperators(Random.nextInt(crossoverOperators.size))

    //Define the two crossover individuals: In case of mutation, we do a crossover with a new random node
    val sourceIndividual = select()
    val targetLinkageRule = if (Random.nextDouble < config.reproduction.mutationProbability) generator() else select().node

    operator(SourceTargetPair(sourceIndividual.node, targetLinkageRule)) match {
      case Some(node) => {
        val startTime = System.currentTimeMillis()

        val fitness = LinkageRuleEvaluator(node.build, referenceEntities)

        val time = System.currentTimeMillis() - startTime

        Individual(node, fitness) //, if(keepHistory) Some(Individual.Base(operator, sourceIndividual)) else None, time)
      }
      case None => {
        //No compatible pairs for this operator found => return unmodified node
        sourceIndividual
      }
    }
  }

  private def select(): Individual = {
    val tournamentNodes = List.fill(config.reproduction.tournamentSize)(individuals(Random.nextInt(individuals.size)))

    tournamentNodes.reduceLeft((n1, n2) => if (n1.fitness.score > n2.fitness.score) n1 else n2)
  }
}