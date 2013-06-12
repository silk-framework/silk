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

package de.fuberlin.wiwiss.silk.entity

import de.fuberlin.wiwiss.silk.util.DPair
import scala.xml.Node

/**
 * Represents a link between two entities.
 *
 * @param source The source URI
 * @param target The target URI
 * @param confidence (Optional) The confidence that this link is correct. Allowed values: [-1.0, 1.0].
 * @param entities (Optional) The entities which are interlinked.
 */
class Link(source: String,
           target: String,
           val confidence: Option[Double] = None,
           val entities: Option[DPair[Entity]] = None) extends DPair[String](source, target) {

  def this(link: Link) = this(link.source, link.target, link.confidence, link.entities)

  /**
   * Reverses the source and the target of this link.
   */
  override def reverse = new Link(target, source, confidence, entities)

  override def toString = "<" + source + ">  <" + target + ">"

  /**
   * Compares two Links for equality.
   * Two Links are considered equal if their source and target URIs match.
   */
  override def equals(other: Any) = other match {
    case otherLink: Link => otherLink.source == source && otherLink.target == target
    case _ => false
  }

  override def hashCode = (source + target).hashCode

  def update(source: String = source,
            target: String = target,
            confidence: Option[Double] = confidence,
            entities: Option[DPair[Entity]] = entities) = new Link(source, target, confidence, entities)

  def toXML = {
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
}

object Link {
  def fromXML(node: Node, entityDescription: Option[EntityDescription]) = {
    new Link(
      source = (node \ "Source").text,
      target = (node \ "Target").text,
      confidence = for(confidenceNode <- (node \ "Confidence").headOption) yield confidenceNode.text.toDouble,
      entities = {
        for(desc <- entityDescription; entitiesNode <- (node \ "Entities").headOption) yield {
          DPair(
            source = Entity.fromXML(entitiesNode \ "Source" head, desc),
            target = Entity.fromXML(entitiesNode \ "Target" head, desc)
          )
        }
      }
    )
  }
}
