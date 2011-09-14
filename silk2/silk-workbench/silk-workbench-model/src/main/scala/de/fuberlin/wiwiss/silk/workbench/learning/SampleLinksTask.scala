package de.fuberlin.wiwiss.silk.workbench.learning

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.learning.individual.Population
import math.log
import de.fuberlin.wiwiss.silk.config.RuntimeConfig
import util.Random
import java.util.logging.Level
import de.fuberlin.wiwiss.silk.evaluation.{ReferenceInstances, LinkageRuleEvaluator}
import de.fuberlin.wiwiss.silk.util.task.ValueTask
import de.fuberlin.wiwiss.silk.GenerateLinksTask
import de.fuberlin.wiwiss.silk.learning.{LearningConfiguration, LearningInput}
import de.fuberlin.wiwiss.silk.learning.generation.{GeneratePopulationTask, LinkageRuleGenerator}
import de.fuberlin.wiwiss.silk.instance.{Instance, Path, InstanceSpecification}
import de.fuberlin.wiwiss.silk.linkspec.{LinkageRule, LinkSpecification}
import de.fuberlin.wiwiss.silk.linkspec.similarity.{DistanceMeasure, Comparison}
import de.fuberlin.wiwiss.silk.linkspec.input.PathInput
import math.max

class SampleLinksTask(sources: Traversable[Source],
                      linkSpec: LinkSpecification,
                      paths: SourceTargetPair[Traversable[Path]],
                      referenceInstances: ReferenceInstances,
                      population: Population) extends ValueTask[Seq[Link]](Seq.empty) {

  private val testLinkCount = 10

  private val testRulesCount = 10

  private val minFMeasure = 0.1

  private val runtimeConfig = RuntimeConfig(useFileCache = false, generateDetailedLinks = true)

  @volatile private var cancelled = false

  /** This linkage rule is included if no good linkage rules have been found in the population */
  private val defaultLinkageRule =
    LinkageRule(Some(Comparison(
      inputs =
        SourceTargetPair(
          source = PathInput(path = Path.parse("?" + linkSpec.datasets.source.variable + "/<http://www.w3.org/2000/01/rdf-schema#label>")),
          target = PathInput(path = Path.parse("?" + linkSpec.datasets.target.variable + "/<http://www.w3.org/2000/01/rdf-schema#label>"))
        ),
      metric = DistanceMeasure("levenshteinDistance"),
      threshold = 1.0
    )))

  def links = value.get

  def execute(): Seq[Link] = {
    cancelled = false

    if (population.isEmpty) {
      if(referenceInstances.negative.isEmpty || referenceInstances.positive.isEmpty) {
        logger.info("No reference links defined. Sampling links from fully random population.")
        val input = LearningInput()
        val config = LearningConfiguration.load()
        val generator = LinkageRuleGenerator(paths, config.components)
        val task = new GeneratePopulationTask(input, generator, config)
        find(task.apply())
      } else {
        logger.info("Learning has not been executed yet. Generating new population to sample from.")
        val input = LearningInput(referenceInstances)
        val config = LearningConfiguration.load()
        val generator = LinkageRuleGenerator(referenceInstances, config.components)
        val task = new GeneratePopulationTask(input, generator, config)
        find(task.apply())
      }
    } else {
      logger.info("Sampling from population of the current learning run.")
      find(population)
    }

    links
  }

  override def stopExecution() {
    cancelled = true
  }

  private def find(population: Population) {
    val linkSpecs = retrieveLinkageRules(population).map(r => linkSpec.copy(rule = r))
    val instanceSpecs = linkSpecs.map(InstanceSpecification.retrieve).reduce((a, b) => SourceTargetPair(a.source merge b.source, a.target merge b.target))
    val sourcePair = linkSpec.datasets.map(_.sourceId).map(id => sources.find(_.id == id).get)

    updateStatus("Sampling")
    for((linkSpec, index) <- linkSpecs.toSeq.zipWithIndex) {
      if (cancelled) return
      val links = generateLinks(sourcePair, linkSpec, instanceSpecs)
      val ratedLinks = rateLinks(sourcePair, linkSpecs, links)
      value.update(value.get ++ ratedLinks)
      updateStatus("Sampling", (index + 1).toDouble / linkSpecs.size)
    }
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

  private def generateLinks(sources: SourceTargetPair[Source], linkSpec: LinkSpecification, instanceSpecPair: SourceTargetPair[InstanceSpecification]) = {
    val generateLinksTask =
      new GenerateLinksTask(sources, linkSpec, Traversable.empty, runtimeConfig) {
        override def instanceSpecs = instanceSpecPair
      }

    generateLinksTask.progressLogLevel = Level.FINE
    generateLinksTask.toTraversable.find(_.size > testLinkCount).getOrElse(generateLinksTask.links).take(testLinkCount)
  }

  private def rateLinks(sources: SourceTargetPair[Source], linkSpecs: Traversable[LinkSpecification], links: Traversable[Link]) = {
    for(link <- links) yield rateLink(sources, linkSpecs, link)
  }

  private def rateLink(sources: SourceTargetPair[Source], linkSpecs: Traversable[LinkSpecification], link: Link) = {

    val a = linkSpecs.map(isLink(_, link)).sum
    val b = linkSpecs.map(s => LinkageRuleEvaluator(s.rule, referenceInstances).fMeasure).sum

    val p = if(b > 0.0) a / b else 0.5

    val entropy = 0.0 - p * log(p) / log(2) - (1 - p) * log(1 - p) / log(2)

    new Link(link.source, link.target, entropy, link.instances.get)
  }

  private def isLink(linkSpec: LinkSpecification, link: Link) = {
    val r = linkSpec.rule(link.instances.get) > 0.0

    val fitness = LinkageRuleEvaluator(linkSpec.rule, referenceInstances)

    //println(r +  " " + fitness + " " + linkSpec.rule)

    if(r) max(fitness.fMeasure, minFMeasure) else 0.0
  }
}

object SampleLinksTask {
  def empty = new SampleLinksTask(Traversable.empty, LinkSpecification(), SourceTargetPair.fill(Traversable.empty), ReferenceInstances.empty, Population())
}