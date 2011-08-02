package de.fuberlin.wiwiss.silk.learning.cleaning

import de.fuberlin.wiwiss.silk.util.ParallelMapper
import de.fuberlin.wiwiss.silk.evaluation.{ReferenceInstances, LinkConditionEvaluator}
import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration
import de.fuberlin.wiwiss.silk.learning.individual._
import de.fuberlin.wiwiss.silk.learning.generation.IndividualGenerator

class CleanPopulationTask(population : Population, instances : ReferenceInstances, generator: IndividualGenerator) extends Task[Population]
{
  /** Maximum difference between two fitness values to be considered equal. */
  private val fitnessEpsilon = 0.0001

  override def execute() : Population =
  {
    val individuals = new ParallelMapper(population.individuals).map(cleanIndividual)

    val distinctIndividuals = removeDuplicates(individuals)

    //println("Removed: " + (individuals.size - distinctIndividuals.size))

    val randomIndividuals = new ParallelMapper(0 until population.individuals.size - distinctIndividuals.size).map{ i =>
      val linkCondition = generator()
      val fitness = LinkConditionEvaluator(linkCondition.build, instances)

      Individual(linkCondition, fitness)
    }

    Population(distinctIndividuals ++ randomIndividuals)
  }

  private def removeDuplicates(individuals : Traversable[Individual]) =
  {
    val sortedIndividuals = individuals.toSeq.sortBy(-_.fitness.score)

    var currentIndividual = sortedIndividuals.head
    var distinctIndividuals = currentIndividual :: Nil

    for(individual <- sortedIndividuals.tail)
    {
      if(!compareLinkConditions(currentIndividual.node, individual.node))
      {
        currentIndividual = individual
        distinctIndividuals ::= individual
      }
    }

    distinctIndividuals
  }

  private def compareLinkConditions(node1 : LinkConditionNode, node2 : LinkConditionNode) = (node1.aggregation, node2.aggregation) match
  {
    case (None, None) => true
    case (Some(op1), Some(op2)) => compareOperators(op1, op2)
    case _ => false
  }

  private def compareOperators(operator1 : Node, operator2 : OperatorNode) : Boolean = (operator1, operator2) match
  {
    case (AggregationNode(agg1, ops1), AggregationNode(agg2, ops2)) =>
    {
      agg1 == agg2 &&
      ops1.forall(op1 => ops2.exists(op2 => compareOperators(op1, op2)))
    }
    case (ComparisonNode(inputs1, limit1, metric1), ComparisonNode(inputs2, limit2, metric2)) =>
    {
      metric1 == metric2 &&
      compareInputs(inputs1.source, inputs2.source) &&
      compareInputs(inputs1.target, inputs2.target)
    }
    case _ => false
  }

  private def compareInputs(input1 : InputNode, input2 : InputNode) : Boolean = (input1, input2) match
  {
    case (PathInputNode(_, path1), PathInputNode(_, path2)) => path1 == path2
    case (TransformNode(_, inputs1, transformer1), TransformNode(_, inputs2, transformer2)) =>
    {
      transformer1 == transformer2 &&
      (inputs1 zip inputs2).forall{ case (i1, i2) => compareInputs(i1, i2) }
    }
    case _ => false
  }

  private def cleanIndividual(individual : Individual) : Individual =
  {
    val root = NodeTraverser(individual.node)
    implicit val fitness = individual.fitness.fMeasure

    val cleanLinkCondition = clean(root).asInstanceOf[LinkConditionNode]

    Individual(cleanLinkCondition, individual.fitness)//, Some(Individual.Base(null, individual)), individual.time)
  }

  /**
   * Recursively traverses through the tree and calls cleanNode() for each node.
   */
  private def clean(location : NodeTraverser)(implicit fitness : Double) : Node =
  {
    val updatedLocation = cleanNode(location)

    val updatedChildren = updatedLocation.iterateChildren.map(clean).toList

    updatedLocation.node.updateChildren(updatedChildren)
  }

  /**
   * Cleans a single node.
   */
  private def cleanNode(location : NodeTraverser)(implicit fitness : Double) : NodeTraverser =
  {
    val updatedLocation = cleanTransformation(location)
    cleanAggregation(updatedLocation)
  }

  /**
   * Removes redundant transformations.
   */
  private def cleanTransformation(location : NodeTraverser)(implicit fitness : Double) : NodeTraverser = location.node match
  {
    case TransformNode(_, input :: Nil, _) =>
    {
      val updatedLocation = location.update(input)

      if(evaluate(updatedLocation) >= fitness - fitnessEpsilon)
      {
        cleanTransformation(updatedLocation)
      }
      else
      {
        location
      }
    }
    case _ => location
  }

  /**
   * Removes all redundant operators of an aggregation node.
   */
  private def cleanAggregation(location : NodeTraverser)(implicit fitness : Double) : NodeTraverser = location.node match
  {
    case AggregationNode(_, (operator : AggregationNode) :: Nil) =>
    {
      cleanAggregation(location.moveDown.get)
    }
    case AggregationNode(_, operator :: Nil) =>
    {
      location.moveDown.get
    }
    case AggregationNode(_, operators) =>
    {
      removeRedundantOperators(location, Nil, operators)
    }
    case _ => location
  }

  private def removeRedundantOperators(node : NodeTraverser, checkedOperators : List[Node], remainingOperators : List[Node])(implicit fitness : Double) : NodeTraverser =
  {
    remainingOperators match
    {
      case head :: tail =>
      {
        val updatedNode = node.update(node.node.updateChildren(checkedOperators ::: tail))

        if(evaluate(updatedNode) < fitness - fitnessEpsilon)
        {
          removeRedundantOperators(node, head :: checkedOperators, tail)
        }
        else
        {
          removeRedundantOperators(updatedNode, checkedOperators, tail)
        }

      }
      case Nil => node
    }
  }

  private def evaluate(node : NodeTraverser) =
  {
    val linkCondition = node.root.node.asInstanceOf[LinkConditionNode]

    LinkConditionEvaluator(linkCondition.build, instances).fMeasure
  }
}