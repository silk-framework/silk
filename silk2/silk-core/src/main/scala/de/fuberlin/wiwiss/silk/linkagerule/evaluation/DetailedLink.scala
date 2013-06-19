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

package de.fuberlin.wiwiss.silk.linkagerule.evaluation

import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.entity.{Entity, Link}
import de.fuberlin.wiwiss.silk.linkagerule.input.{TransformInput, PathInput}
import xml.Node

class DetailedLink(source: String,
                   target: String,
                   entities: Option[DPair[Entity]],
                   val details: Option[DetailedLink.Confidence]) extends Link(source, target, details.flatMap(_.score), entities) {

  def this(link: Link) = this(link.source, link.target, link.entities, link.confidence.map(c => DetailedLink.SimpleConfidence(Some(c))))

  override def toXML =
    <DetailedLink source={source} target={target}>
      { details.map(_.toXML).toList }
    </DetailedLink>
}

object DetailedLink {

  sealed trait Confidence {
    def score: Option[Double]
    def toXML: Node
  }

  case class SimpleConfidence(score: Option[Double]) extends Confidence {
    def toXML =
      <SimpleConfidence score={score.toString}/>
  }

  case class AggregatorConfidence(score: Option[Double], aggregation: Aggregation, children: Seq[Confidence]) extends Confidence {
    def toXML =
      <AggregatorConfidence id={aggregation.id} score={score.toString}>
        { children.map(_.toXML) }
      </AggregatorConfidence>
  }

  case class ComparisonConfidence(score: Option[Double], comparison: Comparison, sourceValue: Value, targetValue: Value) extends Confidence {
    def toXML =
      <ComparisonConfidence id={comparison.id} score={score.toString} sourceValue={sourceValue.values.toString} targetValue={targetValue.values.toString}>
      </ComparisonConfidence>
  }

  sealed trait Value {
    def values: Set[String]
  }
  
  case class TransformedValue(transform: TransformInput, values: Set[String], children: Seq[Value]) extends Value
  
  case class InputValue(input: PathInput, values: Set[String]) extends Value
}