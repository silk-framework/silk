package de.fuberlin.wiwiss.silk.learning.sampling

import de.fuberlin.wiwiss.silk.util.task.ValueTask
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.learning.individual.{Individual, Population}
import math.max
import de.fuberlin.wiwiss.silk.linkagerule.{Operator, LinkageRule}
import de.fuberlin.wiwiss.silk.entity.{Path, Link}
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkagerule.input.{PathInput, TransformInput}

class SampleFromPopulationTask(population: Population, links: Seq[Link]) extends ValueTask[Seq[Link]](Seq.empty) {

  /**
   * Weight the linkage rules by their fMeasure.
   * Better linkage rules will have a bigger weight in the information gain computation.
   */
  val weightedRules = {
    for(individual <- population.individuals) yield {
      new WeightedLinkageRule(individual)
    }
  }

  override protected def execute(): Seq[Link] = {
    val shuffledLinks = for((s, t) <- links zip (links.tail :+ links.head)) yield new Link(s.source, t.target, None, Some(DPair(s.entities.get.source, t.entities.get.target)))

    for(link <- (links ++ shuffledLinks)) yield new Link(link.source, link.target, Some(rate(link)), link.entities)
  }

  def rate(link: Link) = {
    ratingDetails(link)

    val fulfilledRules = weightedRules.filter(rule => rule(link.entities.get) > 0.0)
    val confidence = fulfilledRules.map(_.weight).sum / weightedRules.map(_.weight).sum

    confidence * 2.0 - 1.0
  }

  def ratingDetails(link: Link) {
    for(rule <- weightedRules) {
      println(formatRule(rule) + " " + rule.weight + " " + rule(link.entities.get))
    }
  }

  private def formatRule(rule: LinkageRule) = {
    def collectPaths(op: Operator): Seq[Path] = op match {
      case agg: Aggregation => agg.operators.flatMap(collectPaths)
      case cmp: Comparison => cmp.inputs.flatMap(collectPaths)
      case t: TransformInput => t.inputs.flatMap(collectPaths)
      case i: PathInput => Seq(i.path)
    }

    val paths = collectPaths(rule.operator.get)
    val shortPaths  = paths.map(_.serialize.split("[/#]").last.init)

    shortPaths.mkString(" ")
  }


  class WeightedLinkageRule(individual: Individual) extends LinkageRule(individual.node.build.operator) {
    /** The weight of this linkage rule. Never smaller than 0.1 */
    val weight = math.pow(max(0.1, individual.fitness.mcc), 10.0)
  }
}