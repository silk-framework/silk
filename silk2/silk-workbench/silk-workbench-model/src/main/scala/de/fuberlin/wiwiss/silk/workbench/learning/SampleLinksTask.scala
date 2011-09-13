package de.fuberlin.wiwiss.silk.workbench.learning

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.learning.individual.Population
import math.log
import de.fuberlin.wiwiss.silk.config.RuntimeConfig
import util.Random
import java.util.logging.{LogRecord, Level}
import de.fuberlin.wiwiss.silk.evaluation.{ReferenceInstances, LinkageRuleEvaluator}
import de.fuberlin.wiwiss.silk.util.task.ValueTask
import de.fuberlin.wiwiss.silk.GenerateLinksTask
import de.fuberlin.wiwiss.silk.instance.{Path, InstanceSpecification}
import de.fuberlin.wiwiss.silk.learning.{LearningConfiguration, LearningInput}
import de.fuberlin.wiwiss.silk.learning.generation.{GeneratePopulationTask, LinkageRuleGenerator}

class SampleLinksTask(sources: Traversable[Source],
                      linkSpec: LinkSpecification,
                      paths: SourceTargetPair[Traversable[Path]],
                      referenceInstances: ReferenceInstances,
                      population: Population) extends ValueTask[Seq[Link]](Seq.empty) {

  private val testLinkCount = 10

  private val testRulesCount = 10

  private val runtimeConfig = RuntimeConfig(useFileCache = false)

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

    val sourcePair = linkSpec.datasets.map(_.sourceId).map(id => sources.find(_.id == id).get)

    val testLinks = linkSpecs.flatMap(generateLinks(sourcePair, _)).toSet

    val ratedLinks = rateLinks(sourcePair, linkSpecs, testLinks)

    value.update(ratedLinks.toSeq)
  }

  private def retrieveLinkageRules(population: Population) = {
    val goodIndividuals = population.individuals.filter(_.fitness.fMeasure > 0.1)
    val testIndividuals = Random.shuffle(goodIndividuals).take(testRulesCount)
    val testRules = testIndividuals.map(_.node.build)

    testRules
  }

  private def generateLinks(sources: SourceTargetPair[Source], linkSpec: LinkSpecification) = {
    val generateLinksTask = new GenerateLinksTask(sources, linkSpec, Traversable.empty, runtimeConfig)
    generateLinksTask.progressLogLevel = Level.FINE

    generateLinksTask.toTraversable.find(_.size > testLinkCount).getOrElse(generateLinksTask.links).take(testLinkCount)
  }

  private def rateLinks(sources: SourceTargetPair[Source], linkSpecs: Traversable[LinkSpecification], links: Traversable[Link]) = {
    for(link <- links) yield {
      val entropy = linkEntropy(sources, linkSpecs, link)
      println(link + ": " + entropy)
      new Link(link.source, link.target, entropy)
    }
  }

  private def linkEntropy(sources: SourceTargetPair[Source], linkSpecs: Traversable[LinkSpecification], link: Link) = {
    //val p = linkSpecs.count(isLink(sources, _, link)).toDouble / linkSpecs.size

    val p = linkSpecs.map(isLink(sources, _, link)).sum / linkSpecs.map(s => LinkageRuleEvaluator(s.rule, referenceInstances).fMeasure).sum

    0.0 - p * log(p) / log(2) - (1 - p) * log(1 - p) / log(2)
  }

  private def isLink(sources: SourceTargetPair[Source], linkSpec: LinkSpecification, link: Link) = {
    val instanceSpecs = InstanceSpecification.retrieve(linkSpec)

    val instancePair = retrieveInstancePair(sources, instanceSpecs, link)

    val r = linkSpec.rule(instancePair) > 0.0

    val fitness = LinkageRuleEvaluator(linkSpec.rule, referenceInstances)

    println(r +  " " + fitness + " " + linkSpec.rule)

    if(r) fitness.fMeasure else 0.0
  }

  private def retrieveInstancePair(sources: SourceTargetPair[Source], instanceSpecs: SourceTargetPair[InstanceSpecification], uris: SourceTargetPair[String]) = {
    SourceTargetPair(
      source = sources.source.retrieve(instanceSpecs.source, uris.source :: Nil).head,
      target = sources.target.retrieve(instanceSpecs.target, uris.target :: Nil).head
    )
  }
}

object SampleLinksTask {
  def empty = new SampleLinksTask(Traversable.empty, LinkSpecification(), SourceTargetPair.fill(Traversable.empty), ReferenceInstances.empty, Population())
}