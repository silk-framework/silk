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

package models.linking

import models.linking.EvalLink.{Correctness, LinkType}
import org.silkframework.entity.Link
import org.silkframework.rule.evaluation.EvaluatedLink

/**
 * An evaluation link.
 */
class EvalLink(link: EvaluatedLink,
               val correct: Correctness,
               val linkType: LinkType) extends EvaluatedLink(link.source, link.target, link.linkEntities, link.details) {

  def this(link: Link, correct: Correctness, linkType: LinkType) = {
    this(new EvaluatedLink(link), correct, linkType)
  }

  def updateCorrectness(correct: Correctness) = new EvalLink(this, correct, linkType)
}

object EvalLink {

  /**
   * The correctness of a link
   */
  sealed trait Correctness

  /**
   * Correct link.
   */
  case object Correct extends Correctness

  /**
   * Incorrect link.
   */
  case object Incorrect extends Correctness

  /**
   * Correctness unknown.
   */
  case object Unknown extends Correctness

  /**
   * The type of link.
   */
  sealed trait LinkType {
    def name: String
  }

  /**
   * Link which have been generate by Silk.
   */
  case object Generated extends LinkType {
    override def name: String = "generated"
  }

  /**
   * ReferenceType link.
   */
  trait ReferenceType extends LinkType

  /**
   * Positive reference link.
   */
  case object Positive extends ReferenceType {
    override def name: String = "positive"
  }

  /**
   * Negative reference link.
   */
  case object Negative extends ReferenceType {
    override def name: String = "negative"
  }

  /**
    * Unlabeled reference link.
    */
  case object Unlabeled extends ReferenceType {
    override def name: String = "unlabeled"
  }
}