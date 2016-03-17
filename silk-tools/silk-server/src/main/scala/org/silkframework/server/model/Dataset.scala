/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.server.model

import org.silkframework.cache.MemoryEntityCache
import org.silkframework.config.{LinkSpecification, LinkingConfig}
import org.silkframework.entity.Link
import org.silkframework.execution.{Loader, Matcher}
import org.silkframework.util.DPair

/**
 * Holds the dataset of a link specification.
 */
class Dataset(val name: String, config: LinkingConfig, linkSpec: LinkSpecification, writeUnmatchedEntities: Boolean,
              matchOnlyInProvidedGraph: Boolean) {

  private val sources = linkSpec.dataSelections.map(_.inputId).map(config.source)

  private val entityDescs = linkSpec.entityDescriptions

  private val caches = DPair(new MemoryEntityCache(entityDescs.source, linkSpec.rule.index(_)),
                             new MemoryEntityCache(entityDescs.target, linkSpec.rule.index(_)))

  new Loader(sources, caches)()

  /**
   * Matches a set of entities with all entities in this dataset.
   */
  def apply(source: Source): MatchResult = {
    val matchResult = generateLinks(source)

    MatchResult(
      links = matchResult.links,
      linkType = linkSpec.rule.linkType,
      unmatchedEntities = matchResult.unmatchedEntities
    )
  }

  /**
   * Generates all links where the provided entities are the link source.
   */
  private def generateLinks(source: Source) = {
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
    if (entityCache.size > 0) {
      val matcher = if (matchOnlyInProvidedGraph){
        new Matcher(linkSpec.rule, DPair(entityCache, targetInstanceCache))
      } else {
        new Matcher(linkSpec.rule, DPair(entityCache, caches.target))
      }
      links = matcher()
    }

    val matchedEntities = links.map(_.source).toSet
    val unmatchedEntities = entities.filterNot(entity => matchedEntities.contains(entity.uri))

    if (writeUnmatchedEntities) {
      if (!matchOnlyInProvidedGraph) caches.target.write(unmatchedEntities)
    }

    MatchResult(links, linkSpec.rule.linkType, unmatchedEntities.map(_.uri).toSet)
  }

  def sourceEntityCount = caches.source.size

  def targetEntityCount = caches.target.size
}
