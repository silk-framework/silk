/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.server.model

import de.fuberlin.wiwiss.silk.config.LinkingConfig
import de.fuberlin.wiwiss.silk.{MatchTask, LoadTask}
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.entity.{Link, EntityDescription}
import de.fuberlin.wiwiss.silk.cache.MemoryEntityCache

/**
 * Holds the dataset of a link specification.
 */
class Dataset(val name: String, config: LinkingConfig, linkSpec: LinkSpecification, writeUnmatchedEntities: Boolean,
              matchOnlyInProvidedGraph: Boolean)
                {
  private val sources = linkSpec.datasets.map(_.sourceId).map(config.source(_))

  private val entityDescs = linkSpec.entityDescriptions

  private val caches = DPair(new MemoryEntityCache(entityDescs.source, linkSpec.rule.index(_)),
                             new MemoryEntityCache(entityDescs.target, linkSpec.rule.index(_)))

  new LoadTask(sources, caches)()

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
    val entityCache = new MemoryEntityCache(entityDescs.source, linkSpec.rule.index(_))



    val targetInstanceCache = if (matchOnlyInProvidedGraph){
      new MemoryEntityCache(entityDescs.target, linkSpec.rule.index(_))
    } else {
      caches.target
    }
    
    val entities = source.retrieve(entityDescs.source).toList
    entityCache.write(entities)
    if (matchOnlyInProvidedGraph) {
      val targetEntities = source.retrieve(entityDescs.target).toList
      targetInstanceCache.write(targetEntities)
    }

    var links: Seq[Link] = Seq.empty
    if (entityCache.entityCount > 0) {
      val matcher = if (matchOnlyInProvidedGraph){
        new MatchTask(linkSpec.rule, DPair(entityCache, targetInstanceCache))
      } else {
        new MatchTask(linkSpec.rule, DPair(entityCache, caches.target))  
      }
      links = matcher()
    }

    val matchedEntities = links.map(_.source).toSet
    val unmatchedEntities = entities.filterNot(entity => matchedEntities.contains(entity.uri))

    if (writeUnmatchedEntities) {
      if (!matchOnlyInProvidedGraph) caches.target.write(unmatchedEntities)
    }

    MatchResult(links, linkSpec.linkType, unmatchedEntities.map(_.uri).toSet)
  }

  def sourceEntityCount = caches.source.entityCount

  def targetEntityCount = caches.target.entityCount
}
