package de.fuberlin.wiwiss.silk.server.model

import de.fuberlin.wiwiss.silk.config.SilkConfig
import de.fuberlin.wiwiss.silk.{MatchTask, LoadTask}
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, MemoryInstanceCache}
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

/**
 * Holds the dataset of a link specification.
 */
class Dataset(val name: String, config: SilkConfig, linkSpec: LinkSpecification, writeUnmatchedInstances: Boolean) {
  private val sources = linkSpec.datasets.map(_.sourceId).map(config.source(_))

  private val instanceSpecs = InstanceSpecification.retrieve(linkSpec)

  private val caches = SourceTargetPair(new MemoryInstanceCache(instanceSpecs.source),
    new MemoryInstanceCache(instanceSpecs.target))

  new LoadTask(sources, caches, linkSpec.rule.index(_))()

  /**
   * Matches a set of instances with all instances in this dataset.
   */
  def apply(instanceSource: DataSource): MatchResult = {
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
  private def generateLinks(instanceSource: DataSource) = {
    val instanceCache = new MemoryInstanceCache(instanceSpecs.source)

    val instances = instanceSource.retrieve(instanceSpecs.source).toList
    instanceCache.write(instances, linkSpec.rule.index(_))

    var links: Seq[Link] = Seq.empty
    if (instanceCache.instanceCount > 0) {
      val matcher = new MatchTask(linkSpec, SourceTargetPair(instanceCache, caches.target))
      links = matcher()
    }

    val matchedInstances = links.map(_.source).toSet
    val unmatchedInstances = instances.filterNot(instance => matchedInstances.contains(instance.uri))

    if (writeUnmatchedInstances) {
      //TODO enable blocking
      caches.target.write(unmatchedInstances, linkSpec.rule.index(_))
      //targetCache.write(unmatchedInstances, linkSpec.blocking)
    }

    MatchResult(links, linkSpec.linkType, unmatchedInstances.map(_.uri).toSet)
  }

  def sourceInstanceCount = caches.source.instanceCount

  def targetInstanceCount = caches.target.instanceCount
}
