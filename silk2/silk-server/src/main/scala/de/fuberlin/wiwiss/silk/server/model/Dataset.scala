package de.fuberlin.wiwiss.silk.server.model

import de.fuberlin.wiwiss.silk.config.Configuration
import de.fuberlin.wiwiss.silk.{MatchTask, LoadTask}
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, MemoryInstanceCache}
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.output.Link
import collection.mutable.{Buffer, ArrayBuffer}
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

/**
 * Holds the dataset of a link specification.
 */
class Dataset(val name : String, config : Configuration, linkSpec : LinkSpecification, writeUnmatchedInstances : Boolean)
{
  private val sources = linkSpec.datasets.map(_.sourceId).map(config.source(_))

  private val instanceSpecs = InstanceSpecification.retrieve(linkSpec)

  private val caches = SourceTargetPair.fill(new MemoryInstanceCache())

  new LoadTask(sources, caches, instanceSpecs)()

  /**
   * Matches a set of instances with all instances in this dataset.
   */
  def apply(instanceSource : DataSource) : MatchResult =
  {
    val matchResult = generateLinks(instanceSource)

    MatchResult(
      links = matchResult.links,
      linkType = linkSpec.linkType,
      unmatchedInstances = matchResult.unmatchedInstances
    )
  }

  /**
   * Generates all links where the provided instances are the link source.
   */
  private def generateLinks(instanceSource : DataSource) =
  {
    val instanceCache = new MemoryInstanceCache()

    val instances = instanceSource.retrieve(instanceSpecs.source).toList
    instanceCache.write(instances)

    var links : Buffer[Link] = new ArrayBuffer[Link]()
    if(instanceCache.instanceCount > 0)
    {
      val matcher = new MatchTask(linkSpec, SourceTargetPair(instanceCache, caches.target), 8)
      links = matcher()
    }

    val matchedInstances = links.map(_.sourceUri).toSet
    val unmatchedInstances = instances.filterNot(instance => matchedInstances.contains(instance.uri))

    if(writeUnmatchedInstances)
    {
      //TODO enable blocking
      caches.target.write(unmatchedInstances)
      //targetCache.write(unmatchedInstances, linkSpec.blocking)
    }

    MatchResult(links, linkSpec.linkType, unmatchedInstances.map(_.uri).toSet)
  }

  def sourceInstanceCount = caches.source.instanceCount

  def targetInstanceCount = caches.target.instanceCount
}
