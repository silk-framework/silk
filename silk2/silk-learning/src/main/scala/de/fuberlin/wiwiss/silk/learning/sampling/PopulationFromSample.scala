package de.fuberlin.wiwiss.silk.learning.sampling

import de.fuberlin.wiwiss.silk.learning.LearningInput._
import de.fuberlin.wiwiss.silk.learning.generation.LinkageRuleGenerator._
import de.fuberlin.wiwiss.silk.util.task.Task._
import de.fuberlin.wiwiss.silk.learning.{LearningInput, LearningConfiguration}
import de.fuberlin.wiwiss.silk.learning.generation.{LinkageRuleGenerator, GeneratePopulationTask}
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.linkagerule.{Operator, LinkageRule}
import de.fuberlin.wiwiss.silk.entity.{Path, Link}
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkagerule.input.{PathInput, TransformInput}
import util.Random
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.learning.individual.{Population, Individual}

class PopulationFromSample(links: Seq[Link]) {

  def evaluate() = {
    val input = LearningInput()//TODO Include current linkage rule?
    val config = LearningConfiguration.load()
    val generator = LinkageRuleGenerator(ReferenceEntities.fromEntities(links.map(_.entities.get), Nil), config.components)
    val task = new GeneratePopulationTask(input, generator, config)
    //val population = executeSubTask(task, 5.0, true)
    val population = task()

    Population(population.individuals.filter(i => rateRule(i.node.build)))
  }

  private def rateRule(rule: LinkageRule): Boolean = {
    val entityPairs = links.map(_.entities.get)
    val shuffledEntityPairs = for((s, t) <- entityPairs.map(_.source) zip (entityPairs.tail.map(_.target) :+ entityPairs.head.target)) yield DPair(s, t)

    val count = (entityPairs ++ shuffledEntityPairs).filter(rule(_) > 0).size

    println(formatRule(rule) + ": " + count)

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