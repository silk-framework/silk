package de.fuberlin.wiwiss.silk.workbench.learning

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
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

class SampleLinksTask(sources: Traversable[Source],
                      linkSpec: LinkSpecification,
                      paths: SourceTargetPair[Traversable[Path]],
                      referenceInstances: ReferenceInstances,
                      population: Population) extends ValueTask[Seq[Link]](Seq.empty) {

  private val testLinkCount = 10

  private val testRulesCount = 10

  private val runtimeConfig = RuntimeConfig(useFileCache = false, generateDetailedLinks = true)

  def links = value.get

  def execute(): Seq[Link] = {
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

  private def find(population: Population) {
    val linkSpecs = retrieveLinkageRules(population).map(r => linkSpec.copy(rule = r))
    val instanceSpecs = linkSpecs.map(InstanceSpecification.retrieve).reduce((a, b) => SourceTargetPair(a.source merge b.source, a.target merge b.target))
    val sourcePair = linkSpec.datasets.map(_.sourceId).map(id => sources.find(_.id == id).get)

    updateStatus("Sampling")
    for((linkSpec, index) <- linkSpecs.toSeq.zipWithIndex) {
      val links = generateLinks(sourcePair, linkSpec, instanceSpecs)
      val ratedLinks = rateLinks(sourcePair, linkSpecs, links)
      value.update(value.get ++ ratedLinks)
      updateStatus("Sampling", (index + 1).toDouble / linkSpecs.size)
    }
  }

  private def retrieveLinkageRules(population: Population) = {
    val goodIndividuals = population.individuals.filter(_.fitness.fMeasure > 0.1)
    val testIndividuals = Random.shuffle(goodIndividuals).take(testRulesCount)
    val testRules = testIndividuals.map(_.node.build)

    testRules
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

    val p = linkSpecs.map(isLink(_, link)).sum / linkSpecs.map(s => LinkageRuleEvaluator(s.rule, referenceInstances).fMeasure).sum

    val entropy = 0.0 - p * log(p) / log(2) - (1 - p) * log(1 - p) / log(2)

    //TODO only change entropy
    new Link(link.source, link.target, entropy, link.instances.get)
  }

  private def isLink(linkSpec: LinkSpecification, link: Link) = {
    val r = linkSpec.rule(link.instances.get) > 0.0

    val fitness = LinkageRuleEvaluator(linkSpec.rule, referenceInstances)

    println(r +  " " + fitness + " " + linkSpec.rule)

    if(r) fitness.fMeasure else 0.0
  }
}

object SampleLinksTask {
  def empty = new SampleLinksTask(Traversable.empty, LinkSpecification(), SourceTargetPair.fill(Traversable.empty), ReferenceInstances.empty, Population())
}