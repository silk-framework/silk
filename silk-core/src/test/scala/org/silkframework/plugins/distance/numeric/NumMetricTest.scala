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

package org.silkframework.plugins.distance.numeric

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.testutil.approximatelyEqualTo

@RunWith(classOf[JUnitRunner])
class NumMetricTest extends FlatSpec with Matchers {
  val metric = new NumMetric(minValue = 0.0, maxValue = 1.0)
  val t = 0.9

  "NumMetric" should "return abs(num1 - num2)" in {
    metric.evaluate("0.3", "0.7", t) should be(approximatelyEqualTo(0.4))
    metric.evaluate("0.7", "0.3", t) should be(approximatelyEqualTo(0.4))
  }

  "NumMetric" should "return 0.0 if the numbers are equal" in {
    metric.evaluate("0", "0", t) should be(approximatelyEqualTo(0.0))
    metric.evaluate("123456", "123456", t) should be(approximatelyEqualTo(0.0))
    metric.evaluate("0.3", "0.3", t) should be(approximatelyEqualTo(0.0))
  }
}