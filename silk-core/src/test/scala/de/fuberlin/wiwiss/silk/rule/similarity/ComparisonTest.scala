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

package de.fuberlin.wiwiss.silk.rule.similarity

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.util.{DPair, Identifier}
import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.config.Prefixes
import scala.xml.Node
import de.fuberlin.wiwiss.silk.rule.input.Input
import de.fuberlin.wiwiss.silk.testutil.approximatelyEqualTo

@RunWith(classOf[JUnitRunner])
class ComparisonTest extends FlatSpec with ShouldMatchers {
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
      metric = new DistanceMeasure { def apply(values1: Traversable[String], values2: Traversable[String], limit: Double) = distance },
      inputs = DPair.fill(DummyInput)
    ).apply(null, -1.0).get
  }

  private object DummyInput extends Input {
    val id = Identifier.random
    def apply(entities: DPair[Entity]) = Set("dummy")
    def toXML(implicit prefixes: Prefixes): Node = null
  }
}
