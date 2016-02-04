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

package org.silkframework.evaluation

import org.silkframework.config.Prefixes
import org.silkframework.entity._
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.{Serialization, XmlFormat}
import org.silkframework.util.DPair

import scala.xml.{Node, NodeSeq}

/**
 * Holds the entities which correspond to a set of reference links.
 */
case class ReferenceEntities(sourceEntities: Map[String, Entity] = Map.empty,
                             targetEntities: Map[String, Entity] = Map.empty,
                             positiveLinks: Set[Link] = Set.empty,
                             negativeLinks: Set[Link] = Set.empty,
                             unlabeledLinks: Set[Link] = Set.empty) {

  /** Returns positive and negative reference links. */
  def all = positiveLinks ++ negativeLinks

  /** True, if no entities are available. */
  def isEmpty = positiveLinks.isEmpty && negativeLinks.isEmpty

  /** True, if positive and negative entities are available. */
  def isDefined = positiveLinks.nonEmpty && negativeLinks.nonEmpty

  def positiveEntities: Traversable[DPair[Entity]] = {
    linksToEntities(positiveLinks)
  }

  def negativeEntities: Traversable[DPair[Entity]] = {
    linksToEntities(negativeLinks)
  }

  def unlabeledEntities: Traversable[DPair[Entity]] = {
    linksToEntities(unlabeledLinks)
  }

  // Converts a link to the entity pair.
  private def linksToEntities(links: Set[Link]): Set[DPair[Entity]] = {
    links flatMap linkToEntities
  }

  private def linkToEntities(link: Link): Option[DPair[Entity]] = {
    for {
      sourceEntity <- sourceEntities.get(link.source)
      targetEntity <- targetEntities.get(link.target)
    } yield {
      DPair(sourceEntity, targetEntity)
    }
  }

  /**
   * If and only if the link is a positive link then return the pair of entity.
   * @param link
   * @return
   */
  def positiveLinkToEntities(link: Link): Option[DPair[Entity]] = {
    if (positiveLinks.contains(link)) {
      linkToEntities(link)
    } else {
      None
    }
  }

  def negativeLinkToEntities(link: Link): Option[DPair[Entity]] = {
    if (negativeLinks.contains(link)) {
      linkToEntities(link)
    } else {
      None
    }
  }

  def unlabeledLinkToEntities(link: Link): Option[DPair[Entity]] = {
    if (unlabeledLinks.contains(link)) {
      linkToEntities(link)
    } else {
      None
    }
  }

  /** Merges this reference set with another reference set. */
  def merge(ref: ReferenceEntities) = ReferenceEntities(
    sourceEntities ++ ref.sourceEntities,
    targetEntities ++ ref.targetEntities,
    positiveLinks ++ ref.positiveLinks,
    negativeLinks ++ ref.negativeLinks,
    unlabeledLinks ++ ref.unlabeledLinks
  )

  def update(newSourceEntities: Traversable[Entity] = Traversable.empty,
             newTargetEntities: Traversable[Entity] = Traversable.empty,
             newPositiveLinks: Set[Link] = Set.empty,
             newNegativeLinks: Set[Link] = Set.empty,
             newUnlabeledLinks: Set[Link] = Set.empty) = {
    this.copy(
      sourceEntities = sourceEntities ++ newSourceEntities.map(e => (e.uri, e)),
      targetEntities = targetEntities ++ newTargetEntities.map(e => (e.uri, e)),
      positiveLinks = positiveLinks ++ newPositiveLinks,
      negativeLinks = negativeLinks ++ newNegativeLinks,
      unlabeledLinks = unlabeledLinks ++ newUnlabeledLinks
    )
  }

  /** Retrieves the pair of entity descriptions for the contained entity pairs. */
  def entitySchemas: DPair[EntitySchema] = {
    for {
      sourceEntityDesc <- sourceEntities.values.headOption map (_.desc)
      targetEntityDesc <- targetEntities.values.headOption map (_.desc)
    } {
      return DPair(sourceEntityDesc, targetEntityDesc)
    }
    DPair.fill(EntitySchema.empty)
  }
}

object ReferenceEntities {

  def empty = ReferenceEntities(Map.empty, Map.empty)

  def fromEntities(positiveEntities: Traversable[DPair[Entity]],
                   negativeEntities: Traversable[DPair[Entity]],
                   unlabeledEntities: Traversable[DPair[Entity]] = Traversable.empty) = {
    def srcEnt(e: Traversable[DPair[Entity]]) = e map (_.source) toSet
    def tgtEnt(e: Traversable[DPair[Entity]]) = e map (_.target) toSet

    val sourceEntities = srcEnt(positiveEntities) ++ srcEnt(negativeEntities) ++ srcEnt(unlabeledEntities)
    val targetEntities = tgtEnt(positiveEntities) ++ tgtEnt(negativeEntities) ++ tgtEnt(unlabeledEntities)

    ReferenceEntities(
      sourceEntities = sourceEntities map (e => (e.uri, e)) toMap,
      targetEntities = targetEntities map (e => (e.uri, e)) toMap,
      positiveLinks = positiveEntities.map(i => (new Link(i.source.uri, i.target.uri))).toSet,
      negativeLinks = negativeEntities.map(i => (new Link(i.source.uri, i.target.uri))).toSet,
      unlabeledLinks = unlabeledEntities.map(i => (new Link(i.source.uri, i.target.uri))).toSet
    )
  }

  /**
   * XML serialization format.
   */
  implicit object ReferenceEntitiesFormat extends XmlFormat[ReferenceEntities] {
    /**
     * Deserialize a value from XML.
     */
    def read(node: Node)(implicit prefixes: Prefixes, resources: ResourceManager) = {
      val entityDescs = Serialization.fromXml[DPair[EntitySchema]]((checkAndGet(node,  "Pair").head))

      val sourceEntities = extractEntities(entityDescs.source, checkAndGet(node,  "SourceEntities"))
      val targetEntities = extractEntities(entityDescs.target, checkAndGet(node, "TargetEntities"))
      val positiveLinks: Set[Link] = extractLinks(checkAndGet(node,  "PositiveLinks"))
      val negativeLinks: Set[Link] = extractLinks(checkAndGet(node,  "NegativeLinks"))
      val unlabeledLinks: Set[Link] = extractLinks(checkAndGet(node,  "UnlabeledLinks"))

      ReferenceEntities(sourceEntities, targetEntities, positiveLinks, negativeLinks, unlabeledLinks)
    }

    private def checkAndGet(node: Node, elementName: String): NodeSeq = {
      val elem = node \ elementName
      if(elem.length == 0) {
        throw new RuntimeException(s"Element $elementName not found in XML ReferenceEntities serialization!")
      } else {
        elem
      }
    }

    private def checkNode(node: NodeSeq): Unit = {
      if(node.length == 0) {
        throw new RuntimeException("Mi")
      }
    }

    private def extractEntities(entityDesc: EntitySchema, srcEntNode: NodeSeq): Map[String, Entity] = {
      (for (entityNode <- (srcEntNode \ "Entity")) yield {
        val entity = Entity.fromXML(entityNode, entityDesc)
        (entity.uri, entity)
      }).toMap
    }

    private def extractLinks(linksNode: NodeSeq): Set[Link] = {
      (for (linkNode <- (linksNode \ "LinkCandidate")) yield {
        Link.fromXML(linkNode, None)
      }).toSet
    }

    /**
     * Serialize a value to XML.
     */
    def write(entities: ReferenceEntities)(implicit prefixes: Prefixes): Node = {
      <Entities>
        {Serialization.toXml(entities.entitySchemas)}<SourceEntities>
        {toXML(entities.sourceEntities)}
      </SourceEntities>
        <TargetEntities>
          {toXML(entities.targetEntities)}
        </TargetEntities>
        <PositiveLinks>
          {toXML(entities.positiveLinks)}
        </PositiveLinks>
        <NegativeLinks>
          {toXML(entities.negativeLinks)}
        </NegativeLinks>
        <UnlabeledLinks>
          {toXML(entities.unlabeledLinks)}
        </UnlabeledLinks>
      </Entities>
    }

    private def toXML(links: Set[Link]) = {
      for (link <- links) yield {
        link.toXML
      }
    }

    private def toXML(entities: Map[String, Entity]) = {
      for ((uri, entity) <- entities) yield {
        entity.toXML
      }
    }
  }
}