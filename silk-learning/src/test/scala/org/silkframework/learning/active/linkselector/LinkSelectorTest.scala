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

package org.silkframework.learning.active.linkselector

import org.silkframework.entity._
import org.silkframework.entity.rdf.{SparqlRestriction, SparqlEntitySchema}
import org.silkframework.rule.similarity.{Aggregation, Comparison}
import org.silkframework.util.DPair
import org.silkframework.rule.input.PathInput
import org.silkframework.rule.LinkageRule
import org.silkframework.plugins.aggegrator.MinimumAggregator
import org.silkframework.evaluation.ReferenceEntities
import org.silkframework.plugins.aggegrator.MinimumAggregator
import scala.Some
import org.silkframework.plugins.distance.equality.EqualityMetric
import org.silkframework.plugins.distance.equality.EqualityMetric

object LinkSelectorTest extends App {
  val selector1: LinkSelector = JensenShannonDivergenceSelector(fulfilledOnly = true)
  val selector2: LinkSelector = JensenShannonDivergenceSelector(fulfilledOnly = false)

  val referenceLinks = ReferenceEntities.fromEntities(
    positiveEntities = entities("Frankenstein", "2000", "Frankenstein", "2000") :: Nil,
    negativeEntities = entities("Frankenstein", "2000", "Rambo", "1900") :: entities("Frankenstein", "2000", "Matrix", "2000") :: Nil
  )

  val unlabeledLinks = Seq(
    link("Frankenstein", "2000", "Rambo", "1900"),
    link("Frankenstein", "2000", "Frankenstein", "1900"),
    link("Frankenstein", "2000", "Matrix", "2000"),
    link("Frankenstein", "2000", "Frankenstein", "2000")
  )

  val rules = rule(true, true) :: rule(false, true) :: rule(false, true) :: rule(true, false) :: Nil

  println(selector1(rules, unlabeledLinks, referenceLinks))
  println(selector2(rules, unlabeledLinks, referenceLinks))

  def rule(matchLabel: Boolean, matchDate: Boolean) = {
    def labelComparison =
      Comparison(
        metric = EqualityMetric(),
        inputs = DPair(PathInput(path = Path.parse("?a/<label>")), PathInput(path = Path.parse("?b/<label>")))
      )

    def dateComparison =
      Comparison(
        metric = EqualityMetric(),
        inputs = DPair(PathInput(path = Path.parse("?a/<date>")), PathInput(path = Path.parse("?b/<date>")))
      )

    val operator = (matchLabel, matchDate) match {
      case (false, false) => None
      case (true, false) => Some(labelComparison)
      case (false, true) => Some(dateComparison)
      case (true, true) => {
        Some(
          Aggregation(
            aggregator = MinimumAggregator(),
            operators = Seq(labelComparison, dateComparison)
          )
        )
      }
    }

    new WeightedLinkageRule(operator, 0.0)
  }

  def link(label1: String, date1: String, label2: String, date2: String) = {
    new Link(
      source = label1 + date1,
      target = label2 + date2,
      entities = Some(entities(label1, date1, label2, date2))
    )
  }

  def entities(label1: String, date1: String, label2: String, date2: String) = {
    val sourceEntityDesc = SparqlEntitySchema("a", SparqlRestriction.empty, IndexedSeq(Path.parse("?a/<label>"), Path.parse("?a/<date>")))
    val targetEntityDesc = SparqlEntitySchema("b", SparqlRestriction.empty, IndexedSeq(Path.parse("?b/<label>"), Path.parse("?b/<date>")))

    DPair(
      source = new Entity(label1 + date1, IndexedSeq(Seq(label1), Seq(date1)), sourceEntityDesc),
      target = new Entity(label2 + date2, IndexedSeq(Seq(label2), Seq(date2)), targetEntityDesc)
    )
  }
}