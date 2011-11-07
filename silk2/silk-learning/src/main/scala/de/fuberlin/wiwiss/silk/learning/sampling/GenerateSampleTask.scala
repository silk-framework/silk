package de.fuberlin.wiwiss.silk.learning.sampling

import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.learning.individual.Population._
import de.fuberlin.wiwiss.silk.util.task.ValueTask
import de.fuberlin.wiwiss.silk.{LoadTask, GenerateLinksTask}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimilarityOperator
import de.fuberlin.wiwiss.silk.util.{Identifier, DPair}
import de.fuberlin.wiwiss.silk.plugins.metric.LevenshteinDistance._
import de.fuberlin.wiwiss.silk.entity.{Index, Entity, Link, Path}
import xml.Node
import de.fuberlin.wiwiss.silk.config.RuntimeConfig._
import de.fuberlin.wiwiss.silk.config.{RuntimeConfig, Prefixes, LinkSpecification}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule._
import de.fuberlin.wiwiss.silk.plugins.metric.{EqualityMetric, LevenshteinDistance}
import de.fuberlin.wiwiss.silk.linkagerule.input.PathInput
import util.Random
import de.fuberlin.wiwiss.silk.util.RandomUtils._

class GenerateSampleTask(sources: Traversable[Source],
                      linkSpec: LinkSpecification,
                      paths: DPair[Seq[Path]],
                      referenceEntities: ReferenceEntities = ReferenceEntities.empty,
                      var population: Population = Population()) extends ValueTask[Seq[Link]](Seq.empty) {

  private val runtimeConfig = RuntimeConfig(partitionSize = 100, useFileCache = false, generateLinksWithEntities = true)

  def links = value.get

  override protected def execute(): Seq[Link] = {
    sampleLinks()
  }

  var generateLinksTask: GenerateLinksTask = _

  private def sampleLinks() = {
    val entityDesc = DPair(linkSpec.entityDescriptions.source.copy(paths = paths.source.toIndexedSeq),
                           linkSpec.entityDescriptions.target.copy(paths = paths.target.toIndexedSeq))

    val op = new TestOperator()
    val linkSpec2 = linkSpec.copy(rule = LinkageRule(op))

    generateLinksTask =
      new GenerateLinksTask(sources, linkSpec2, Traversable.empty, runtimeConfig) {
        override def entityDescs = entityDesc
      }

    val listener = (v: Seq[Link]) =>  {
      value.update(v)
      if(v.size > 1000) generateLinksTask.cancel()
    }
    generateLinksTask.value.onUpdate(listener)
    updateStatus(0.0)
    executeSubTask(generateLinksTask, 0.8, true)


    var links = op.getLinks()

    //TODO check if any links have been loaded and warn

    if(population.isEmpty) {
      updateStatus("Generating population")
      population = new PopulationFromSample(links).evaluate()
    }

    updateStatus("Sampling", 0.9)

//    val link = links.find(l => l.source == "http://dbpedia.org/resource/Topaz_%281969_film%29" && l.target == "http://data.linkedmdb.org/resource/film/230").get
//    val rating = rate(link)
//    println(rating)


    val filteredPopulation = FilterPopulation(population, links)

    links = new SampleFromPopulationTask(filteredPopulation, links).apply()

    value.update(links)

    links
  }

  private class TestOperator() extends SimilarityOperator {

    val links = Array.fill(paths.source.size, paths.target.size)(Seq[Link]())

    def getLinks() = {
      val a = links.flatten.flatten
      val c = a.groupBy(_.source).values.map(randomElement(_))
               .groupBy(_.target).values.map(randomElement(_))
      Random.shuffle(c.toSeq).take(1000)
    }

    val metric = EqualityMetric()

    val maxDistance = 0.0

    /** Maximum number of indices per property. If a property has more indices the remaining indices are ignored. */
    val maxIndices = 5

    def apply(entities: DPair[Entity], limit: Double = 0.0): Option[Double] = {
      for((sourcePath, sourceIndex) <- paths.source.zipWithIndex;
          (targetPath, targetIndex) <- paths.target.zipWithIndex) {
        val sourceValues = entities.source.evaluate(sourcePath)
        val targetValues = entities.target.evaluate(targetPath)
        val size = links(sourceIndex)(targetIndex).size
        val labelLinks = links(0)(0).size

//        if(entities.source.uri == "http://dbpedia.org/resource/Jagged_Edge_%28film%29" && entities.target.uri == "http://data.linkedmdb.org/resource/film/1932") {
//          links(sourceIndex)(targetIndex) :+= new Link(source = entities.source.uri, target = entities.target.uri, entities = Some(entities))
//        }

//        if(entities.source.uri == "http://dbpedia.org/resource/Topaz_%281969_film%29" && entities.target.uri == "http://data.linkedmdb.org/resource/film/230") {
//          links(sourceIndex)(targetIndex) :+= new Link(source = entities.source.uri, target = entities.target.uri, entities = Some(entities))
//        }

        if(size <= 1000 && metric(sourceValues, targetValues, maxDistance) <= maxDistance) {
          links(sourceIndex)(targetIndex) :+= new Link(source = entities.source.uri, target = entities.target.uri, entities = Some(entities))
          //println(sourceIndex + " - " + targetIndex)
        }

        if (size > 1000 && labelLinks > 100)
          generateLinksTask.cancel()
      }

      None
    }

    val id = Identifier.random

    val required = false

    val weight = 1

    def index(entity: Entity, limit: Double): Index = {
      val entities = DPair.fill(entity)

      val allPaths = paths.source.toSet ++ paths.target.toSet

      val inputs = allPaths.map(p => PathInput(path = p))

      val index = inputs.map(i => i(entities)).map(metric.index(_, maxDistance).crop(maxIndices)).reduce(_ merge _)

      index
    }

    def toXML(implicit prefixes: Prefixes): Node = throw new UnsupportedOperationException("Cannot serialize " + getClass.getName)
  }
}

object GenerateSampleTask {
  def empty = new GenerateSampleTask(Traversable.empty, LinkSpecification(), DPair.fill(Seq.empty), ReferenceEntities.empty, Population())
}