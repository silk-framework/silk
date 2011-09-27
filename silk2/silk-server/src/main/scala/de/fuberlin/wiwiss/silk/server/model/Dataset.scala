package de.fuberlin.wiwiss.silk.server.model

import de.fuberlin.wiwiss.silk.config.LinkingConfig
import de.fuberlin.wiwiss.silk.{MatchTask, LoadTask}
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.entity.{EntityDescription, MemoryEntityCache}
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.util.DPair

/**
 * Holds the dataset of a link specification.
 */
class Dataset(val name: String, config: LinkingConfig, linkSpec: LinkSpecification, writeUnmatchedEntities: Boolean) {
  private val sources = linkSpec.datasets.map(_.sourceId).map(config.source(_))

  private val entityDescs = EntityDescription.retrieve(linkSpec)

  private val caches = DPair(new MemoryEntityCache(entityDescs.source),
    new MemoryEntityCache(entityDescs.target))

  new LoadTask(sources, caches, linkSpec.rule.index(_))()

  /**
   * Matches a set of entities with all entities in this dataset.
   */
  def apply(source: DataSource): MatchResult = {
    val matchResult = generateLinks(source)

    MatchResult(
      links = matchResult.links,
      linkType = linkSpec.linkType,
      unmatchedEntities = matchResult.unmatchedEntities
    )
  }

  /**
   * Generates all links where the provided entities are the link source.
   */
  private def generateLinks(source: DataSource) = {
    val entityCache = new MemoryEntityCache(entityDescs.source)

    val entities = source.retrieve(entityDescs.source).toList
    entityCache.write(entities, linkSpec.rule.index(_))

    var links: Seq[Link] = Seq.empty
    if (entityCache.entityCount > 0) {
      val matcher = new MatchTask(linkSpec, DPair(entityCache, caches.target))
      links = matcher()
    }

    val matchedEntities = links.map(_.source).toSet
    val unmatchedEntities = entities.filterNot(entity => matchedEntities.contains(entity.uri))

    if (writeUnmatchedEntities) {
      //TODO enable blocking
      caches.target.write(unmatchedEntities, linkSpec.rule.index(_))
      //targetCache.write(unmatchedEntities, linkSpec.blocking)
    }

    MatchResult(links, linkSpec.linkType, unmatchedEntities.map(_.uri).toSet)
  }

  def sourceEntityCount = caches.source.entityCount

  def targetEntityCount = caches.target.entityCount
}
