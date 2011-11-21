/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

private class SampleFromPopulationTask(population: Population, unlabeledLinks: Seq[Link], referenceEntities: ReferenceEntities) extends ValueTask[Seq[Link]](Seq.empty) {

  /**
   * Weight the linkage rules.
   * Better linkage rules will have a bigger weight in the information gain computation.
   */
  private val weightedRules = {
    val bestFitness = population.individuals.map(_.fitness).max
    val topIndividuals = population.individuals.toSeq.filter(_.fitness >= bestFitness * 0.5)
    for(individual <- topIndividuals) yield {
      new WeightedLinkageRule(individual)
    }
  }

  val positivePoints = {
    for((link, entityPair) <- referenceEntities.positive) yield {
      weightedRules.map(_.apply(entityPair))
    }
  }

  val negativePoints = {
    for((link, entityPair) <- referenceEntities.negative) yield {
      weightedRules.map(_.apply(entityPair))
    }
  }

  override protected def execute(): Seq[Link] = {
    //println("AGREEMENT: " + links.map(entropy).minBy(_.abs))
    //println("MAXDIST: " + links.map(distance2).max)

    val valLinks = for(link <- unlabeledLinks) yield link.update(confidence = Some(rate(link)))
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
    val weight = max(0.0001, individual.fitness)
  }
}