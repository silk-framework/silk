package de.fuberlin.wiwiss.silk.learning.sampling

import de.fuberlin.wiwiss.silk.util.task.ValueTask
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.learning.individual.{Individual, Population}
import math.max

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
    val fulfilledRules = weightedRules.filter(rule => rule(link.entities.get) > 0.0)
    val confidence = fulfilledRules.map(_.weight).sum / weightedRules.map(_.weight).sum

    confidence * 2.0 - 1.0
  }

  class WeightedLinkageRule(individual: Individual) extends LinkageRule(individual.node.build.operator) {
    /** The weight of this linkage rule. Never smaller than 0.1 */
    val weight = max(0.1, individual.fitness.fMeasure)
  }
}