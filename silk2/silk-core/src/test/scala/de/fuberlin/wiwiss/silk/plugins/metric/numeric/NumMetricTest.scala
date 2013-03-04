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

package de.fuberlin.wiwiss.silk.plugins.metric.numeric

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.plugins.util.approximatelyEqualTo

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NumMetricTest extends FlatSpec with ShouldMatchers {
  val metric = new NumMetric(minValue = 0.0, maxValue = 1.0)
  val t = 0.9

  "NumMetric" should "return (threshold - abs(num1 - num2)) / threshold" in {
    metric.evaluate("0.3", "0.7", t) should be(approximatelyEqualTo(0.60))
    metric.evaluate("0.7", "0.3", t) should be(approximatelyEqualTo(0.60))
  }

  "NumMetric" should "return 1.0 if the numbers are equal" in {
    metric.evaluate("0", "0", t) should be(approximatelyEqualTo(1.0))
    metric.evaluate("123456", "123456", t) should be(approximatelyEqualTo(1.0))
    metric.evaluate("0.3", "0.3", t) should be(approximatelyEqualTo(1.0))
  }

  "NumMetric" should "return 0.0 if one number is 0" in {
    metric.evaluate("0", "1", t) should be(approximatelyEqualTo(0.0))
    metric.evaluate("1", "0", t) should be(approximatelyEqualTo(0.0))
  }
}