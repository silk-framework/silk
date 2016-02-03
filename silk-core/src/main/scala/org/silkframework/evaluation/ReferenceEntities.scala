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
import scala.xml.Node

/**
 * Holds the entities which correspond to a set of reference links.
 */
case class ReferenceEntities(positive: Map[Link, DPair[Entity]] = Map.empty,
                             negative: Map[Link, DPair[Entity]] = Map.empty,
                             unlabeled: Map[Link, DPair[Entity]] = Map.empty) {

  /** Returns positive and negative reference links. */
  def all = positive ++ negative

  /** True, if no entities are available. */
  def isEmpty = positive.isEmpty && negative.isEmpty

  /** True, if positive and negative entities are available. */
  def isDefined = positive.nonEmpty && negative.nonEmpty

  /** Merges this reference set with another reference set. */
  def merge(ref: ReferenceEntities) =  ReferenceEntities(positive ++ ref.positive, negative ++ ref.negative, unlabeled ++ ref.unlabeled)

  def withPositive(entityPair: DPair[Entity]) = {
    copy(positive = positive + (new Link(entityPair.source.uri, entityPair.target.uri) -> entityPair))
  }

  def withNegative(entityPair: DPair[Entity]) = {
    copy(negative = negative + (new Link(entityPair.source.uri, entityPair.target.uri) -> entityPair))
  }

  def withUnlabeled(entityPair: DPair[Entity]) = {
    copy(unlabeled = unlabeled + (new Link(entityPair.source.uri, entityPair.target.uri) -> entityPair))
  }

  /** Retrieves the pair of entity descriptions for the contained entity pairs. */
  def entitiyDescs: DPair[EntitySchema] = {
    (positive ++ negative ++ unlabeled).values.headOption match {
      case Some(entityPair) => entityPair.map(_.desc)
      case None => DPair.fill(EntitySchema.empty)
    }
  }
}

object ReferenceEntities {

  def empty = ReferenceEntities(Map.empty, Map.empty)

  def fromEntities(positiveEntities: Traversable[DPair[Entity]], negativeEntities: Traversable[DPair[Entity]], unlabeledEntities: Traversable[DPair[Entity]] = Traversable.empty) = {
    ReferenceEntities(
      positive = positiveEntities.map(i => (new Link(i.source.uri, i.target.uri), i)).toMap,
      negative = negativeEntities.map(i => (new Link(i.source.uri, i.target.uri), i)).toMap,
      unlabeled = unlabeledEntities.map(i => (new Link(i.source.uri, i.target.uri), i)).toMap
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
      val posNode = node \ "PositiveEntities"
      val negNode = node \ "NegativeEntities"
      val unlabeledNode = node \ "UnlabeledEntities"

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

      val unlabeledEntities: Traversable[DPair[Entity]] = {
        if (unlabeledNode.isEmpty) {
          Traversable.empty
        } else {
          for (pairNode <- (unlabeledNode \ "Pair").toList) yield {
            DPair(
              Entity.fromXML((pairNode \ "Source" \ "Entity").head, entityDescs.source),
              Entity.fromXML((pairNode \ "Target" \ "Entity").head, entityDescs.target))
          }
        }
      }

      ReferenceEntities.fromEntities(positiveEntities, negativeEntities, unlabeledEntities)
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
        </PositiveEntities>
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
        </NegativeEntities>
        <UnlabeledEntities>
          {for (DPair(sourceEntity, targetEntity) <- entities.unlabeled.values) yield {
          <Pair>
            <Source>
              {sourceEntity.toXML}
            </Source>
            <Target>
              {targetEntity.toXML}
            </Target>
          </Pair>
        }}
        </UnlabeledEntities>
      </Entities>
    }
  }
}