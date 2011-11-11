package de.fuberlin.wiwiss.silk.learning.active

import de.fuberlin.wiwiss.silk.util.task.ValueTask
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.learning.individual.{Individual, Population}
import math.max
import de.fuberlin.wiwiss.silk.linkagerule.{Operator, LinkageRule}
import de.fuberlin.wiwiss.silk.entity.{Path, Link}
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkagerule.input.{PathInput, TransformInput}
import de.fuberlin.wiwiss.silk.evaluation.{ReferenceEntities, ReferenceLinks}
import math.{pow, sqrt, min, max}

private class SampleFromPopulationTask(population: Population, links: Seq[Link], referenceEntities: ReferenceEntities) extends ValueTask[Seq[Link]](Seq.empty) {

  private val shuffledLinks = for((s, t) <- links zip (links.tail :+ links.head)) yield new Link(s.source, t.target, None, Some(DPair(s.entities.get.source, t.entities.get.target)))

  /**
   * Weight the linkage rules.
   * Better linkage rules will have a bigger weight in the information gain computation.
   */
  private val weightedRules = {
    val bestScore = population.individuals.map(_.fitness.score).max
    val topIndividuals = population.individuals.toSeq.filter(_.fitness.score >= bestScore * 0.5)
    for(individual <- topIndividuals) yield {
      new WeightedLinkageRule(individual)
    }
  }

  val positivePoints = {
    if(!referenceEntities.positive.isEmpty) {
      for((link, entityPair) <- referenceEntities.positive) yield {
        weightedRules.map(_.apply(entityPair))
      }
    } else {
      val entityPair = links.map(withAgreement).maxBy(_.confidence).entities.get
      Seq(weightedRules.map(_.apply(entityPair)))
    }
  }

  val negativePoints = {
    if(!referenceEntities.negative.isEmpty) {
      for((link, entityPair) <- referenceEntities.negative) yield {
        weightedRules.map(_.apply(entityPair))
      }
    } else {
      val entityPair = shuffledLinks.map(withAgreement).minBy(_.confidence).entities.get
      Seq(weightedRules.map(_.apply(entityPair)))
    }
  }

  override protected def execute(): Seq[Link] = {
    println("AGREEMENT: " + links.map(entropy).minBy(_.abs))
    println("MAXDIST: " + links.map(distance2).max)

    val valLinks = for(link <- (links ++ shuffledLinks)) yield link.update(confidence = Some(rate(link)))
    valLinks.sortBy(_.confidence.get.abs).take(3)
  }

  def withAgreement(link: Link) = {
    val confidenceSum = weightedRules.map(_.apply(link.entities.get)).sum
    val confidence = confidenceSum / weightedRules.size

    link.update(confidence = Some(confidence))
  }

  def rate(link: Link) = {
    //entropy(link)
    certainty(link)
  }

  def entropy(link: Link) = {
    val fulfilledRules = weightedRules.filter(rule => rule(link.entities.get) > 0.0)
    val confidence = fulfilledRules.map(_.weight).sum / weightedRules.map(_.weight).sum

    confidence * 2.0 - 1.0
  }

  def certainty(link: Link) = {
    val c = weightedRules.map(_.apply(link.entities.get))

    val posDist = positivePoints.map(distance(_, c)).min
    val negDist = negativePoints.map(distance(_, c)).min

    (negDist - posDist) / (posDist + negDist)
  }

  def distance2(link: Link) = {
    val c = weightedRules.map(_.apply(link.entities.get))

    val minDist = (positivePoints ++ negativePoints).map(distance(_, c)).min

    minDist
  }

  def distance(v1: Seq[Double], v2: Seq[Double]) = {
    sqrt((v1 zip v2).map(p => pow(p._1 - p._2, 2.0)).sum) / (2.0 * weightedRules.size)
  }

  class WeightedLinkageRule(individual: Individual) extends LinkageRule(individual.node.build.operator) {
    /** The weight of this linkage rule. Never smaller than 0.0001 */
    val weight = max(0.0001, individual.fitness.mcc)
  }
}