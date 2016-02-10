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



import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.testutil.approximatelyEqualTo


class InsideNumericIntervalTest extends FlatSpec with Matchers {

  val m = new InsideNumericInterval()

  "InsideNumericInterval" should "return 0.0 if a number is inside an interval" in {
    m.evaluate("5", "3-6") should be(approximatelyEqualTo(0.0))
    m.evaluate("999", "2–100000") should be(approximatelyEqualTo(0.0))
    m.evaluate("6", "3-6") should be(approximatelyEqualTo(0.0))
    m.evaluate("3-6", "3") should be(approximatelyEqualTo(0.0))
  }

  "InsideNumericInterval" should "return 1.0 if a number is outside an interval" in {
    m.evaluate("7", "3-6") should be(approximatelyEqualTo(1.0))
    m.evaluate("1", "2–100000") should be(approximatelyEqualTo(1.0))
    m.evaluate("2", "3-6") should be(approximatelyEqualTo(1.0))
    m.evaluate("3-6", "2") should be(approximatelyEqualTo(1.0))
  }

  "InsideNumericInterval" should "return 0.0 if there are two equal numbers" in {
    m.evaluate("5", "5") should be(approximatelyEqualTo(0.0))
    m.evaluate("999", "999") should be(approximatelyEqualTo(0.0))
  }

  "InsideNumericInterval" should "return 1.0 if there are two unequal numbers" in {
    m.evaluate("6", "5") should be(approximatelyEqualTo(1.0))
    m.evaluate("999", "1000") should be(approximatelyEqualTo(1.0))
  }

  "InsideNumericInterval" should "return 0.0 if an interval is inside or equal another interval" in {
    m.evaluate("3-6", "3-6") should be(approximatelyEqualTo(0.0))
    m.evaluate("3-4", "3-6") should be(approximatelyEqualTo(0.0))
    m.evaluate("3-6", "4-5") should be(approximatelyEqualTo(0.0))
  }

  "InsideNumericInterval" should "return 1.0 if an interval is not inside another interval" in {
    m.evaluate("2-5", "3-6") should be(approximatelyEqualTo(1.0))
    m.evaluate("2-5", "4-6") should be(approximatelyEqualTo(1.0))
  }
}