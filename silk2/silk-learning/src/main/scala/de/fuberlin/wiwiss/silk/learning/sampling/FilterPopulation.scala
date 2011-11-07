package de.fuberlin.wiwiss.silk.learning.sampling

import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.util.DPair._
import de.fuberlin.wiwiss.silk.linkagerule.{Operator, LinkageRule}
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkagerule.input.{PathInput, TransformInput}
import de.fuberlin.wiwiss.silk.entity.{Link, Path}
import de.fuberlin.wiwiss.silk.util.DPair

object FilterPopulation {
  def apply(population: Population, links: Seq[Link]) = {
    Population(population.individuals.filter(i => rateRule(i.node.build, links)))
  }

  private def rateRule(rule: LinkageRule, links: Seq[Link]): Boolean = {
    val entityPairs = links.map(_.entities.get)
    val shuffledEntityPairs = for((s, t) <- entityPairs.map(_.source) zip (entityPairs.tail.map(_.target) :+ entityPairs.head.target)) yield DPair(s, t)

    val count = (entityPairs ++ shuffledEntityPairs).filter(rule(_) > 0).size

    println(formatRule(rule) + ": " + count + "/" + links.size)

    count > 0 && count <= links.size
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
}