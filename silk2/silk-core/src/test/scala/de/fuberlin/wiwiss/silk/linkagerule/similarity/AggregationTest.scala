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

package de.fuberlin.wiwiss.silk.linkagerule.similarity

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.plugins.aggegrator.AverageAggregator
import de.fuberlin.wiwiss.silk.util.{DPair, Identifier}
import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.entity.Index
import de.fuberlin.wiwiss.silk.config.Prefixes
import xml.Node

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import de.fuberlin.wiwiss.silk.testutil.approximatelyEqualToOption

@RunWith(classOf[JUnitRunner])
class AggregationTest extends FlatSpec with ShouldMatchers {
  val aggregator = new AverageAggregator()

  "Aggregation with average aggregator" should "return the average if both operators return a value" in {
    eval(
      operator(_required = false, value = Some(0.8)) ::
      operator(_required = false, value = Some(0.6)) :: Nil
    ) should be(approximatelyEqualToOption(Some(0.7)))
  }

  "Aggregation with average aggregator" should "return the average of all values if the operators not returning a value are not required" in {
    eval(
      operator(_required = false, value = Some(0.8)) ::
      operator(_required = false, value = Some(0.6)) ::
      operator(_required = false, value = None) :: Nil
    ) should be(approximatelyEqualToOption(Some(0.7)))
  }

  "Aggregation with average aggregator" should "return nothing if a required operator does not return a value" in {
    eval(
      operator(_required = false, value = Some(0.8)) ::
      operator(_required = false, value = Some(0.6)) ::
      operator(_required = true, value = None) :: Nil
    ) should be(approximatelyEqualToOption(None))
  }

  "Aggregation with average aggregator" should "aggregate the indices" in {
    index(
      operator(_required = false, indices = Index.default) ::
      operator(_required = false, indices = Index.empty) :: Nil
    ) should equal (Index.default)
  }

  private def eval(ops: Seq[SimilarityOperator]) = {
    Aggregation(operators = ops, aggregator = aggregator).apply(null)
  }

  private def index(ops: Seq[SimilarityOperator]) = {
    Aggregation(operators = ops, aggregator = aggregator).index(null, 0.0)
  }

  private def operator(_weight: Int = 1, _required: Boolean = false, value: Option[Double] = None, indices: Index = Index.default) = {
    new SimilarityOperator {
      val id: Identifier = Identifier.random
      val weight: Int = _weight
      val required: Boolean = _required
      val indexing: Boolean = true
      def apply(entities: DPair[Entity], limit: Double): Option[Double] = value
      def index(entity: Entity, limit: Double): Index = indices
      def toXML(implicit prefixes: Prefixes): Node = null
    }
  }
}