package de.fuberlin.wiwiss.silk.learning.sampling

import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.learning.individual.Population
import math.log
import util.Random
import de.fuberlin.wiwiss.silk.evaluation.{ReferenceEntities, LinkageRuleEvaluator}
import de.fuberlin.wiwiss.silk.util.task.ValueTask
import de.fuberlin.wiwiss.silk.GenerateLinksTask
import de.fuberlin.wiwiss.silk.learning.{LearningConfiguration, LearningInput}
import de.fuberlin.wiwiss.silk.learning.generation.{GeneratePopulationTask, LinkageRuleGenerator}
import math.max
import de.fuberlin.wiwiss.silk.util.{Identifier, DPair}
import de.fuberlin.wiwiss.silk.entity._
import de.fuberlin.wiwiss.silk.config.{Prefixes, LinkSpecification, RuntimeConfig}
import xml.Node
import de.fuberlin.wiwiss.silk.linkagerule.{Operator, LinkageRule}
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Aggregation, SimilarityOperator, DistanceMeasure, Comparison}
import de.fuberlin.wiwiss.silk.linkagerule.input.{TransformInput, PathInput}

class SampleLinksTask(sources: Traversable[Source],
                      linkSpec: LinkSpecification,
                      paths: DPair[Traversable[Path]],
                      referenceEntities: ReferenceEntities,
                      population: Population) extends ValueTask[Seq[Link]](Seq.empty) {

  private val testRulesCount = 30

  private val runtimeConfig = RuntimeConfig(partitionSize = 100, useFileCache = false, generateLinksWithEntities = true)

  //TODO include current linkage rule into sampling?
  /** This linkage rule is included if no good linkage rules have been found in the population */
  private val defaultLinkageRule =
    LinkageRule(Some(Comparison(
      inputs =
        DPair(
          source = PathInput(path = Path.parse("?" + linkSpec.datasets.source.variable + "/<http://www.w3.org/2000/01/rdf-schema#label>")),
          target = PathInput(path = Path.parse("?" + linkSpec.datasets.target.variable + "/<http://www.w3.org/2000/01/rdf-schema#label>"))
        ),
      metric = DistanceMeasure("levenshteinDistance"),
      threshold = 0.0
    )))

  def links = value.get

  def execute(): Seq[Link] = {
    if (population.isEmpty) {
      if(referenceEntities.negative.isEmpty || referenceEntities.positive.isEmpty) {
        logger.info("No reference links defined. Sampling links from fully random population.")
        val input = LearningInput()//Include current linkage rule
        val config = LearningConfiguration.load()
        val generator = LinkageRuleGenerator(paths, config.components)
        val task = new GeneratePopulationTask(input, generator, config)
        find(task.apply())
      } else {
        logger.info("Learning has not been executed yet. Generating new population to sample from.")
        val input = LearningInput(referenceEntities)//Include current linkage rule
        val config = LearningConfiguration.load()
        val generator = LinkageRuleGenerator(referenceEntities, config.components)
        val task = new GeneratePopulationTask(input, generator, config)
        find(task.apply())
      }
    } else {
      logger.info("Sampling from population of the current learning run.")
      find(population)
    }

    links
  }

  private def find(population: Population) {
    val rules = retrieveLinkageRules(population)
    val linkSpecs = rules.map(r => linkSpec.copy(rule = r))
    val entityDescPair = linkSpecs.map(_.entityDescriptions).reduce((e1, e2) => DPair(e1.source merge e2.source, e1.target merge e2.target))

    val informationGainOp = new InformationGainOperator(linkSpecs.map(_.rule))
    val informationGainLinkSpec = linkSpec.copy(rule = LinkageRule(informationGainOp))

    val generateLinksTask =
      new GenerateLinksTask(sources, informationGainLinkSpec, Traversable.empty, runtimeConfig) {
        override def entityDescs = entityDescPair
      }

    executeSubValueTask(generateLinksTask)

    informationGainOp.print()
  }

  private def retrieveLinkageRules(population: Population) = {
    //Find all individuals with a minimum f-Measure
    val individuals = population.individuals.filter(_.fitness.fMeasure > 0.1).toSeq

    var rules = individuals.map(_.node.build)
    rules = Random.shuffle(rules).take(testRulesCount)
    if(rules.size < testRulesCount)
      rules = Seq(defaultLinkageRule) ++ rules ++ Random.shuffle(population.individuals.map(_.node.build)).take(testRulesCount - rules.size - 1)

    rules
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

  /**
   * Computes the information gain of knowing that two given entities match.
   */
  private class InformationGainOperator(rules: Traversable[LinkageRule]) extends SimilarityOperator {
    /**
     * Weight the linkage rules by their fMeasure.
     * Better linkage rules will have a bigger weight in the information gain computation.
     */
    private val weightedRules = {
      for(rule <- rules.toSeq) yield {
        new WeightedLinkegRule(rule)
      }
    }

    def print() {
      for(rule <- weightedRules) {
        println(formatRule(rule) + ": " + rule.posCount + "|" + rule.negCount)
      }
    }

    /**
     * Returns the information gain of knowing if the given entities match.
     */
    def apply(entities: DPair[Entity], limit: Double = 0.0): Option[Double] = {
      val fulfilledRules = weightedRules.filter(rule => rule.eval(entities) > 0.0)

      if(fulfilledRules.isEmpty) {
        None
      }
      else {

        //for(rule <- rules) {
        //  println(rule(entities) + ": " + formatRule(rule))
        //}

        val p = fulfilledRules.map(_.weight).sum / weightedRules.map(_.weight).sum
        val entropy = 0.0 - p * log(p) / log(2) - (1 - p) * log(1 - p) / log(2)

        //println(entropy)

        if(entropy > 0.5) Some(entropy) else None
      }
    }

    val id = Identifier.random

    val required = false

    val weight = 1

    def index(entity: Entity, limit: Double): Index = {

      Index.default

      //rules.map(_.index(entity, limit)).reduce(_ merge _)
    }

    def toXML(implicit prefixes: Prefixes): Node = throw new UnsupportedOperationException("Cannot serialize " + getClass.getName)
  }

  class WeightedLinkegRule(rule: LinkageRule) extends LinkageRule(rule.operator) {
    var posCount = 0
    var negCount = 0

    def eval(entities: DPair[Entity]) = {
      val sim = apply(entities)
      if(sim > 0.0)
        posCount += 1
      else
        negCount += 1
      sim
    }

    /** The weight of this linkage rule. Never smaller than 0.1 */
    val weight = max(0.1, LinkageRuleEvaluator(rule, referenceEntities).fMeasure)
  }
}

object SampleLinksTask {
  def empty = new SampleLinksTask(Traversable.empty, LinkSpecification(), DPair.fill(Traversable.empty), ReferenceEntities.empty, Population())
}