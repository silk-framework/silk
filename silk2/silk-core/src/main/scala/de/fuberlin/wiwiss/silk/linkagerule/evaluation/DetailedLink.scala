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

package de.fuberlin.wiwiss.silk.linkagerule.evaluation

import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkagerule.input.PathInput
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.entity.{Entity, Link}

class DetailedLink(source: String,
                   target: String,
                   entities: Option[DPair[Entity]],
                   val details: Option[DetailedLink.Confidence]) extends Link(source, target, details.flatMap(_.value), entities) {

  def this(link: Link) = this(link.source, link.target, link.entities, link.confidence.map(c => DetailedLink.SimpleConfidence(Some(c))))
}

object DetailedLink {

  sealed trait Confidence {
    def value: Option[Double]
  }

  case class SimpleConfidence(value: Option[Double]) extends Confidence

  case class AggregatorConfidence(value: Option[Double], aggregation: Aggregation, children: Seq[Confidence]) extends Confidence

  case class ComparisonConfidence(value: Option[Double], comparison: Comparison, sourceInput: InputValue, targetInput: InputValue) extends Confidence

  case class InputValue(input: PathInput, values: Traversable[String])

}