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

package org.silkframework.rule.evaluation

import org.silkframework.entity.{Entity, Link, LinkDecision, LinkWithDecision}
import org.silkframework.util.DPair

import scala.xml.Elem

/**
  * Link with evaluation details.
  */
trait LinkWithEvaluation extends Link {

  def details: Confidence

}

class EvaluatedLink(val source: String,
                    val target: String,
                    val linkEntities: DPair[Entity],
                    val details: Confidence) extends LinkWithEvaluation {

  def this(link: Link) = this(link.source, link.target, link.entities.getOrElse(throw new IllegalArgumentException("No entities available")), SimpleConfidence(link.confidence))

  def withDecision(decision: LinkDecision): EvaluatedLinkWithDecision = {
    EvaluatedLinkWithDecision(source, target, linkEntities, details, decision)
  }

  override def toXML: Elem =
    <DetailedLink source={source} target={target}>
      { details.toXML }
    </DetailedLink>

  override def confidence: Option[Double] = details.score

  override def entities: Option[DPair[Entity]] = Some(linkEntities)

  override def reverse: Link = new EvaluatedLink(target, source, linkEntities.reverse, details)
}

case class EvaluatedLinkWithDecision(source: String,
                                     target: String,
                                     linkEntities: DPair[Entity],
                                     details: Confidence,
                                     decision: LinkDecision) extends LinkWithEvaluation with LinkWithDecision {

  override def confidence: Option[Double] = details.score

  override def entities: Option[DPair[Entity]] = Some(linkEntities)

  override def reverse: Link = EvaluatedLinkWithDecision(target, source, linkEntities.reverse, details, decision)

}