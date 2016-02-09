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

package org.silkframework.rule.similarity



import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.Prefixes
import org.silkframework.entity.Entity
import org.silkframework.rule.Operator
import org.silkframework.rule.input.Input
import org.silkframework.testutil.approximatelyEqualTo
import org.silkframework.util.{DPair, Identifier}

import scala.xml.Node


class ComparisonTest extends FlatSpec with Matchers {
  "Comparison" should "return the distance normalized to [-1, 1]" in {
    cmp(distance = 1.0, threshold = 1.0) should be(approximatelyEqualTo(0.0))
    cmp(distance = 4.0, threshold = 4.0) should be(approximatelyEqualTo(0.0))
    cmp(distance = 1.0, threshold = 2.0) should be(approximatelyEqualTo(0.5))
    cmp(distance = 0.5, threshold = 1.0) should be(approximatelyEqualTo(0.5))
    cmp(distance = 0.0, threshold = 1.0) should be(approximatelyEqualTo(1.0))
    cmp(distance = 1.5, threshold = 1.0) should be(approximatelyEqualTo(-0.5))
    cmp(distance = 2.0, threshold = 1.0) should be(approximatelyEqualTo(-1.0))
  }
  
  private def cmp(distance: Double, threshold: Double) = {
    Comparison(
      threshold = threshold,
      metric = new DistanceMeasure { def apply(values1: Seq[String], values2: Seq[String], limit: Double) = distance },
      inputs = DPair.fill(DummyInput)
    ).apply(DPair.fill(null), -1.0).get
  }

  private object DummyInput extends Input {
    val id = Identifier.random
    def apply(entity: Entity): Seq[String] = Seq("dummy")
    def toXML(implicit prefixes: Prefixes): Node = null
    def children = Seq.empty
    def withChildren(newChildren: Seq[Operator]) = ???
  }
}
