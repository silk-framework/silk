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

package org.silkframework.entity

import org.silkframework.util.DPair

import scala.xml.{Elem, Node}

trait Link {
  /** The source entity URI */
  def source: String

  /** The target entity URI */
  def target: String

  /** The optional confidence value */
  def confidence: Option[Double]

  /** The optional pair of entities (values) */
  def entities: Option[DPair[Entity]]

  /** Flip the source and target entity */
  def reverse: Link

  /** Serialize to XML */
  def toXML: Elem = {
    <LinkCandidate>
      <Source>{source}</Source>
      <Target>{target}</Target>
      { for(c <- confidence) yield <Confidence>{c}</Confidence> }
      { for(e <- entities) yield {
      <Entities>
        <Source>{e.source.toXML}</Source>
        <Target>{e.target.toXML}</Target>
      </Entities>
    }
      }
    </LinkCandidate>
  }

  /** Update the link */
  def update(source: String = source,
             target: String = target,
             confidence: Option[Double] = confidence,
             entities: Option[DPair[Entity]] = entities): Link = Link(source, target, confidence, entities)

  override def hashCode: Int = (source + target).hashCode

  override def toString: String = "<" + source + ">  <" + target + ">"

  /**
    * Compares two Links for equality.
    * Two Links are considered equal if their source and target URIs match.
    */
  override def equals(other: Any): Boolean = other match {
    case otherLink: Link => otherLink.source == source && otherLink.target == target
    case _ => false
  }
}

/** Link with only the source and target entity URIs */
class MinimalLink(val source: String, val target: String) extends Link {
  override def confidence: Option[Double] = None

  override def entities: Option[DPair[Entity]] = None

  override def reverse: Link = new MinimalLink(target, source)
}

class LinkWithConfidence(val source: String, val target: String, conf: Double) extends Link {
  override def confidence: Option[Double] = Some(conf)

  override def entities: Option[DPair[Entity]] = None

  override def reverse: Link = new LinkWithConfidence(target, source, conf)
}

class LinkWithEntities(val source: String, val target: String, ents: DPair[Entity]) extends Link {
  override def confidence: Option[Double] = None

  override def entities: Option[DPair[Entity]] = Some(ents)

  override def reverse: Link = new LinkWithEntities(target, source, ents)
}

/**
 * Represents a link between two entities.
 *
 * @param source The source URI
 * @param target The target URI
 * @param _confidence The confidence that this link is correct. Allowed values: [-1.0, 1.0].
 * @param _entities The entities which are interlinked.
 */
class FullLink(val source: String,
               val target: String,
               _confidence: Double,
               _entities: DPair[Entity]) extends Link {
  /**
   * Reverses the source and the target of this link.
   */
  override def reverse: FullLink = new FullLink(target, source, _confidence, _entities)

  override def confidence: Option[Double] = Some(_confidence)

  override def entities: Option[DPair[Entity]] = Some(_entities)
}

object Link {

  def apply(source: String,
            target: String,
            confidence: Option[Double] = None,
            entities: Option[DPair[Entity]] = None): Link = {
    (confidence, entities) match {
      case (None, None) => new MinimalLink(source, target)
      case (Some(conf), None) => new LinkWithConfidence(source, target, conf)
      case (None, Some(es)) => new LinkWithEntities(source, target, es)
      case (Some(conf), Some(es)) => new FullLink(source, target, conf, es)
    }
  }

  def fromXML(node: Node, entityDescription: Option[EntitySchema]): Link = {
    val source = (node \ "Source").text
    val target = (node \ "Target").text
    val confidence = for(confidenceNode <- (node \ "Confidence").headOption) yield confidenceNode.text.toDouble
    val entities = {
      for(desc <- entityDescription; entitiesNode <- (node \ "Entities").headOption) yield {
        DPair(
          source = Entity.fromXML((entitiesNode \ "Source").head, desc),
          target = Entity.fromXML((entitiesNode \ "Target").head, desc)
        )
      }
    }
    Link(source, target, confidence, entities)
  }
}