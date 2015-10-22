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

package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.entity._
import de.fuberlin.wiwiss.silk.entity.rdf.SparqlEntitySchema
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceLoader
import de.fuberlin.wiwiss.silk.runtime.serialization.{Serialization, XmlFormat}
import de.fuberlin.wiwiss.silk.util.DPair
import scala.xml.Node

/**
 * Holds the entities which correspond to a set of reference links.
 */
case class ReferenceEntities(positive: Map[Link, DPair[Entity]] = Map.empty,
                             negative: Map[Link, DPair[Entity]] = Map.empty) {

  /** Returns positive and negative reference links. */
  def all = positive ++ negative

  /** True, if no entities are available. */
  def isEmpty = positive.isEmpty && negative.isEmpty

  /** True, if positive and negative entities are available. */
  def isDefined = positive.nonEmpty && negative.nonEmpty

  /** Merges this reference set with another reference set. */
  def merge(ref: ReferenceEntities) =  ReferenceEntities(positive ++ ref.positive, negative ++ ref.negative)

  def withPositive(entityPair: DPair[Entity]) = {
    copy(positive = positive + (new Link(entityPair.source.uri, entityPair.target.uri) -> entityPair))
  }

  def withNegative(entityPair: DPair[Entity]) = {
    copy(negative = negative + (new Link(entityPair.source.uri, entityPair.target.uri) -> entityPair))
  }

  /** Retrieves the pair of entity descriptions for the contained entity pairs. */
  def entitiyDescs: DPair[SparqlEntitySchema] = {
    (positive ++ negative).values.headOption match {
      case Some(entityPair) => entityPair.map(_.desc)
      case None => DPair.fill(SparqlEntitySchema.empty)
    }
  }
}

object ReferenceEntities {
  def empty = ReferenceEntities(Map.empty, Map.empty)

  def fromEntities(positiveEntities: Traversable[DPair[Entity]], negativeEntities: Traversable[DPair[Entity]]) = {
    ReferenceEntities(
      positive = positiveEntities.map(i => (new Link(i.source.uri, i.target.uri), i)).toMap,
      negative = negativeEntities.map(i => (new Link(i.source.uri, i.target.uri), i)).toMap
    )
  }

  /**
   * XML serialization format.
   */
  implicit object ReferenceEntitiesFormat extends XmlFormat[ReferenceEntities] {
    /**
     * Deserialize a value from XML.
     */
    def read(node: Node)(implicit prefixes: Prefixes, resourceLoader: ResourceLoader) = {
      val entityDescs = Serialization.fromXml[DPair[SparqlEntitySchema]]((node \ "Pair").head)
      val posNode = node \ "PositiveEntities"
      val negNode = node \ "NegativeEntities"

      val positiveEntities: Traversable[DPair[Entity]] = {
        if (posNode.isEmpty) {
          Traversable.empty
        } else {
          for (pairNode <- (posNode \ "Pair").toList) yield {
            DPair(
              Entity.fromXML((pairNode \ "Source" \ "Entity").head, entityDescs.source),
              Entity.fromXML((pairNode \ "Target" \ "Entity").head, entityDescs.target))
          }
        }
      }

      val negativeEntities: Traversable[DPair[Entity]] = {
        if (negNode.isEmpty) {
          Traversable.empty
        } else {
          for (pairNode <- (negNode \ "Pair").toList) yield {
            DPair(
              Entity.fromXML((pairNode \ "Source" \ "Entity").head, entityDescs.source),
              Entity.fromXML((pairNode \ "Target" \ "Entity").head, entityDescs.target))
          }
        }
      }

      ReferenceEntities.fromEntities(positiveEntities, negativeEntities)
    }

    /**
     * Serialize a value to XML.
     */
    def write(entities: ReferenceEntities)(implicit prefixes: Prefixes): Node = {
      <Entities>
        { Serialization.toXml(entities.entitiyDescs) }
        <PositiveEntities>
          {for (DPair(sourceEntity, targetEntity) <- entities.positive.values) yield {
          <Pair>
            <Source>
              {sourceEntity.toXML}
            </Source>
            <Target>
              {targetEntity.toXML}
            </Target>
          </Pair>
        }}
        </PositiveEntities>)
        <NegativeEntities>
          {for (DPair(sourceEntity, targetEntity) <- entities.negative.values) yield {
          <Pair>
            <Source>
              {sourceEntity.toXML}
            </Source>
            <Target>
              {targetEntity.toXML}
            </Target>
          </Pair>
        }}
        </NegativeEntities>)
      </Entities>
    }
  }
}