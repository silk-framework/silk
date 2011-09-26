package de.fuberlin.wiwiss.silk.workbench.learning

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.learning.individual.Population
import math.log
import de.fuberlin.wiwiss.silk.config.RuntimeConfig
import util.Random
import java.util.logging.Level
import de.fuberlin.wiwiss.silk.evaluation.{ReferenceEntities, LinkageRuleEvaluator}
import de.fuberlin.wiwiss.silk.util.task.ValueTask
import de.fuberlin.wiwiss.silk.GenerateLinksTask
import de.fuberlin.wiwiss.silk.learning.{LearningConfiguration, LearningInput}
import de.fuberlin.wiwiss.silk.learning.generation.{GeneratePopulationTask, LinkageRuleGenerator}
import de.fuberlin.wiwiss.silk.entity.{Entity, Path, EntityDescription}
import math.max
import de.fuberlin.wiwiss.silk.linkspec.{Operator, LinkageRule, LinkSpecification}
import de.fuberlin.wiwiss.silk.linkspec.similarity.{Aggregation, DistanceMeasure, Comparison}
import de.fuberlin.wiwiss.silk.linkspec.input.{TransformInput, PathInput}

class SampleLinksTask(sources: Traversable[Source],
                      linkSpec: LinkSpecification,
                      paths: SourceTargetPair[Traversable[Path]],
                      referenceEntities: ReferenceEntities,
                      population: Population) extends ValueTask[Seq[Link]](Seq.empty) {

  private val testLinkCount = 10

  private val testRulesCount = 30

  private val minFMeasure = 0.1

  private val runtimeConfig = RuntimeConfig(useFileCache = false, generateDetailedLinks = true)

  @volatile private var currentGenerateLinksTask: Option[GenerateLinksTask] = None

  @volatile private var canceled = false

  //TODO include current link spec into sampling?
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
    canceled = false

    if (population.isEmpty) {
      if(referenceEntities.negative.isEmpty || referenceEntities.positive.isEmpty) {
        logger.info("No reference links defined. Sampling links from fully random population.")
        val input = LearningInput()
        val config = LearningConfiguration.load()
        val generator = LinkageRuleGenerator(paths, config.components)
        val task = new GeneratePopulationTask(input, generator, config)
        find(task.apply())
      } else {
        logger.info("Learning has not been executed yet. Generating new population to sample from.")
        val input = LearningInput(referenceEntities)
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

  override def stopExecution() {
    canceled = true
    currentGenerateLinksTask.map(_.cancel())
  }

  private def find(population: Population) {
    val linkSpecs = retrieveLinkageRules(population).map(r => linkSpec.copy(rule = r))
    val entityDescs = linkSpecs.map(EntityDescription.retrieve).reduce((a, b) => SourceTargetPair(a.source merge b.source, a.target merge b.target))
    val sourcePair = linkSpec.datasets.map(_.sourceId).map(id => sources.find(_.id == id).get)

    updateStatus("Sampling")
    for((linkSpec, index) <- linkSpecs.toSeq.zipWithIndex) {
      if (canceled) return
      val links = generateLinks(sourcePair, linkSpec, entityDescs)
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

    def collectPaths(op: Operator): Seq[Path] = op match {
      case agg: Aggregation => agg.operators.flatMap(collectPaths)
      case cmp: Comparison => cmp.inputs.flatMap(collectPaths)
      case t: TransformInput => t.inputs.flatMap(collectPaths)
      case i: PathInput => Seq(i.path)
    }

//    for (rule <- rules) {
//      println(collectPaths(rule.operator.get).mkString)
//    }
    //println(rules.mkString("\n"))

    rules
  }

  private def generateLinks(sources: SourceTargetPair[Source], linkSpec: LinkSpecification, entityDescPair: SourceTargetPair[EntityDescription]) = {
    val generateLinksTask =
      new GenerateLinksTask(sources, linkSpec, Traversable.empty, runtimeConfig) {
        override def entityDescs = entityDescPair
      }

    currentGenerateLinksTask = Some(generateLinksTask)
    generateLinksTask.progressLogLevel = Level.FINE
    generateLinksTask.toTraversable.find(_.size > testLinkCount).getOrElse(generateLinksTask.links).take(testLinkCount)
  }

  private def rateLinks(sources: SourceTargetPair[Source], linkSpecs: Traversable[LinkSpecification], links: Traversable[Link]) = {
    for(link <- links) yield rateLink(sources, linkSpecs, link)
  }

  private def rateLink(sources: SourceTargetPair[Source], linkSpecs: Traversable[LinkSpecification], link: Link) = {

    val a = linkSpecs.map(isLink(_, link)).sum
    val b = linkSpecs.map(s => LinkageRuleEvaluator(s.rule, referenceEntities).fMeasure).sum

    val p = if(b > 0.0) a / b else 0.5

    val entropy = 0.0 - p * log(p) / log(2) - (1 - p) * log(1 - p) / log(2)

    new Link(link.source, link.target, entropy, link.entities.get)
  }

  private def isLink(linkSpec: LinkSpecification, link: Link) = {
    val r = linkSpec.rule(link.entities.get) > 0.
    val fitness = LinkageRuleEvaluator(linkSpec.rule, referenceEntities)

//    println(r +  " " + fitness + " " + link)
//    println(linkSpec.rule)

    if(r) max(fitness.fMeasure, minFMeasure) else 0.0
  }
}

object SampleLinksTask {
  def empty = new SampleLinksTask(Traversable.empty, LinkSpecification(), SourceTargetPair.fill(Traversable.empty), ReferenceEntities.empty, Population())
}