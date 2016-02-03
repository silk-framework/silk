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
import org.silkframework.entity.rdf.SparqlEntitySchema
import org.silkframework.runtime.resource.{ResourceManager, ResourceLoader}
import org.silkframework.runtime.serialization.{Serialization, XmlFormat}
import org.silkframework.util.DPair
import scala.xml.{NodeSeq, Node}

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
    if(positiveLinks.contains(link)) {
      linkToEntities(link)
    } else {
      None
    }
  }

  def negativeLinkToEntities(link: Link): Option[DPair[Entity]] = {
    if(negativeLinks.contains(link)) {
      linkToEntities(link)
    } else {
      None
    }
  }

  def unlabeledLinkToEntities(link: Link): Option[DPair[Entity]] = {
    if(unlabeledLinks.contains(link)) {
      linkToEntities(link)
    } else {
      None
    }
  }

  // Converts a link to the entity pair.
  private def linksToEntities(links: Set[Link]): Set[DPair[Entity]] = {
    links flatMap linkToEntities
  }

  /** Merges this reference set with another reference set. */
  def merge(ref: ReferenceEntities) = ReferenceEntities(
    sourceEntities ++ ref.sourceEntities,
    targetEntities ++ ref.targetEntities,
    positiveLinks ++ ref.positiveLinks,
    negativeLinks ++ ref.negativeLinks,
    unlabeledLinks ++ ref.unlabeledLinks
  )

  private def updateEntities(entityPair: DPair[Entity],
                             referenceEntities: ReferenceEntities): ReferenceEntities = {
    referenceEntities.copy(
      sourceEntities = sourceEntities + (entityPair.source.uri -> entityPair.source),
      targetEntities = targetEntities + (entityPair.target.uri -> entityPair.target)
    )
  }

  def withPositive(entityPair: DPair[Entity]) = {
    updateEntities(
      entityPair,
      copy(
        positiveLinks = positiveLinks + (new Link(entityPair.source.uri, entityPair.target.uri))
      )
    )
  }

  def withNegative(entityPair: DPair[Entity]) = {
    updateEntities(
      entityPair,
      copy(
        negativeLinks = negativeLinks + (new Link(entityPair.source.uri, entityPair.target.uri))
      )
    )
  }

  def withUnlabeled(entityPair: DPair[Entity]) = {
    updateEntities(
      entityPair,
      copy(
        unlabeledLinks = unlabeledLinks + (new Link(entityPair.source.uri, entityPair.target.uri))
      )
    )
  }

  /** Retrieves the pair of entity descriptions for the contained entity pairs. */
  def entityDescs: DPair[EntitySchema] = {
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
      val entityDescs = Serialization.fromXml[DPair[EntitySchema]]((node \ "Pair").head)

      val sourceEntities = extractEntities(entityDescs.source, node \ "SourceEntities")
      val targetEntities = extractEntities(entityDescs.target, node \ "TargetEntities")
      val positiveLinks: Set[Link] = extractLinks(node \ "PositiveLinks")
      val negativeLinks: Set[Link] = extractLinks(node \ "NegativeLinks")
      val unlabeledLinks: Set[Link] = extractLinks(node \ "UnlabeledLinks")

      ReferenceEntities(sourceEntities, targetEntities, positiveLinks, negativeLinks, unlabeledLinks)
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
        {Serialization.toXml(entities.entityDescs)}<SourceEntities>
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