package de.fuberlin.wiwiss.silk.server.model

import de.fuberlin.wiwiss.silk.config.Configuration
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.{MatchTask, LoadTask}
import de.fuberlin.wiwiss.silk.impl.writer.MemoryWriter
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, MemoryInstanceCache}
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification

/**
 * Holds the dataset of a link specification.
 */
class Dataset(val name : String, config : Configuration, linkSpec : LinkSpecification, writeUnmatchedInstances : Boolean)
{
  private val sourceCache = new MemoryInstanceCache()
  private val targetCache = new MemoryInstanceCache()
  new LoadTask(config, linkSpec, Some(sourceCache), Some(targetCache))()

  private val instanceSpecs = InstanceSpecification.retrieve(config, linkSpec)

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
    val writer = new MemoryWriter()

    val instances = instanceSource.retrieve(instanceSpecs.source).toList
    instanceCache.write(instances)
    if(instanceCache.instanceCount > 0)
    {
      val matcher = new MatchTask(config.copy(outputs = Nil), linkSpec.copy(outputs = new Output(writer) :: Nil), instanceCache, targetCache)
      matcher()
    }

    val matchedInstances = writer.links.map(_.sourceUri).toSet
    val unmatchedInstances = instances.filterNot(instance => matchedInstances.contains(instance.uri))

    if(writeUnmatchedInstances)
    {
      targetCache.write(unmatchedInstances, linkSpec.blocking)
    }

    MatchResult(writer.links, linkSpec.linkType, unmatchedInstances.map(_.uri).toSet)
  }

  def sourceInstanceCount = sourceCache.instanceCount

  def targetInstanceCount = targetCache.instanceCount
}
