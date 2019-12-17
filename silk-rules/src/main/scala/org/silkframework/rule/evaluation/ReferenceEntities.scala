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

package org.silkframework.rule.evaluation

import java.io.OutputStream
import javax.xml.stream.XMLStreamReader
import javax.xml.transform.{Transformer, TransformerFactory}

import org.silkframework.entity._
import org.silkframework.runtime.serialization.{ReadContext, StreamXmlFormat, XmlSerialization}
import org.silkframework.util.XMLUtils.toXMLUtils
import org.silkframework.util.DPair

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
 *
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
             newUnlabeledLinks: Set[Link] = Set.empty): ReferenceEntities = {
    this.copy(
      sourceEntities = sourceEntities ++ newSourceEntities.map(e => (e.uri.toString, e)),
      targetEntities = targetEntities ++ newTargetEntities.map(e => (e.uri.toString, e)),
      positiveLinks = positiveLinks ++ newPositiveLinks,
      negativeLinks = negativeLinks ++ newNegativeLinks,
      unlabeledLinks = unlabeledLinks ++ newUnlabeledLinks
    )
  }

  /** Retrieves the pair of entity descriptions for the contained entity pairs. */
  def entitySchemas: DPair[EntitySchema] = {
    for {
      sourceEntityDesc <- sourceEntities.values.headOption map (_.schema)
      targetEntityDesc <- targetEntities.values.headOption map (_.schema)
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
                   unlabeledEntities: Traversable[DPair[Entity]] = Traversable.empty): ReferenceEntities = {
    def srcEnt(e: Traversable[DPair[Entity]]) = e.map(_.source).toSet
    def tgtEnt(e: Traversable[DPair[Entity]]) = e.map(_.target).toSet

    val sourceEntities = srcEnt(positiveEntities) ++ srcEnt(negativeEntities) ++ srcEnt(unlabeledEntities)
    val targetEntities = tgtEnt(positiveEntities) ++ tgtEnt(negativeEntities) ++ tgtEnt(unlabeledEntities)

    ReferenceEntities(
      sourceEntities = sourceEntities.map(e => (e.uri.toString, e)).toMap,
      targetEntities = targetEntities.map(e => (e.uri.toString, e)).toMap,
      positiveLinks = positiveEntities.map(i => new MinimalLink(i.source.uri, i.target.uri)).toSet,
      negativeLinks = negativeEntities.map(i => new MinimalLink(i.source.uri, i.target.uri)).toSet,
      unlabeledLinks = unlabeledEntities.map(i => new MinimalLink(i.source.uri, i.target.uri)).toSet
    )
  }

  /**
   * XML serialization format.
   */
  implicit object ReferenceEntitiesFormat extends StreamXmlFormat[ReferenceEntities] {
    final val ENTITY = "Entity"
    final val ENTITIES = "Entities"
    final val SOURCE_ENTITIES = "SourceEntities"
    final val TARGET_ENTITIES = "TargetEntities"
    final val POSITIVE_LINKS = "PositiveLinks"
    final val NEGATIVE_LINKS = "NegativeLinks"
    final val UNLABELED_LINKS = "UnlabeledLinks"

    /**
     * Deserialize a value from XML.
     */
    override def read(implicit streamReader: XMLStreamReader, readContext: ReadContext): ReferenceEntities = {
      val transformerFactory = TransformerFactory.newInstance()
      implicit val transformer: Transformer = transformerFactory.newTransformer
      placeOnStartTag("Pair")
      val entityDescs = readNextObject[DPair[EntitySchema]]
      placeOnNextTagAfterStartTag(SOURCE_ENTITIES)
      val sourceEntities = extractEntities(entityDescs.source)
      placeOnNextTagAfterStartTag(TARGET_ENTITIES)
      val targetEntities = extractEntities(entityDescs.source)
      placeOnNextTagAfterStartTag(POSITIVE_LINKS)
      val positiveLinks = extractLinks
      placeOnNextTagAfterStartTag(NEGATIVE_LINKS)
      val negativeLinks = extractLinks
      placeOnNextTagAfterStartTag(UNLABELED_LINKS)
      val unlabeledLinks = extractLinks

      ReferenceEntities(sourceEntities, targetEntities, positiveLinks, negativeLinks, unlabeledLinks)
    }

    private def extractEntities(entityDesc: EntitySchema)
                               (implicit transformer: Transformer, streamReader: XMLStreamReader, readContext: ReadContext): Map[String, Entity] = {
      val entities = convertObjects(expectedTag = Some("Entity"), convert = (node) => {
        Entity.fromXML(node, entityDesc)
      })
      entities.map(e => (e.uri.toString, e)).toMap
    }

    private def extractLinks(implicit transformer: Transformer, streamReader: XMLStreamReader, readContext: ReadContext): Set[Link] = {
      val links = convertObjects(expectedTag = Some("LinkCandidate"), convert = (node) => {
        Link.fromXML(node, None)
      })
      links.toSet
    }

    /**
     * Serialize a value to XML.
     */
    override def write(entities: ReferenceEntities, outputStream: OutputStream): Unit = {
      implicit val os: OutputStream = outputStream
      withTag(ENTITIES) {
        XmlSerialization.toXml(entities.entitySchemas).write(outputStream)
        withTag(SOURCE_ENTITIES) {
          writeEntities(entities.sourceEntities)
        }
        withTag(TARGET_ENTITIES) {
          writeEntities(entities.targetEntities)
        }
        withTag(POSITIVE_LINKS) {
          writeLinks(entities.positiveLinks)
        }
        withTag(NEGATIVE_LINKS) {
          writeLinks(entities.negativeLinks)
        }
        withTag(UNLABELED_LINKS) {
          writeLinks(entities.unlabeledLinks)
        }
      }
    }

    private def writeLinks(links: Set[Link])(implicit outputStream: OutputStream): Unit = {
      for (link <- links) yield {
        link.toXML.write(outputStream)
      }
    }

    private def writeEntities(entities: Map[String, Entity])(implicit outputStream: OutputStream): Unit = {
      for ((_, entity) <- entities) yield {
        entity.toXML.write(outputStream)
      }
    }
  }
}