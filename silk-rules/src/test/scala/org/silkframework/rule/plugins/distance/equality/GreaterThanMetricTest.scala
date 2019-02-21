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

package org.silkframework.rule.plugins.distance.equality

import org.silkframework.test.PluginTest


class GreaterThanMetricTest extends PluginTest {

  lazy val greater = GreaterThanMetric()
  lazy val greaterOrEqual = GreaterThanMetric(orEqual = true)
  lazy val greaterReversed = GreaterThanMetric(reverse = true)
  val t = 1.0

  behavior of "GreaterThanMetric"

  it should "always return 1.0 if the source number is smaller than the target number" in {
    greater.evaluate("1", "2", t) shouldBe 1.0
    greater.evaluate("0.123", "0.223", t) shouldBe 1.0
    greater.evaluate("5", "20", t) shouldBe 1.0
    greaterOrEqual.evaluate("1", "2", t) shouldBe 1.0
    greaterOrEqual.evaluate("0.1", "0.2", t) shouldBe 1.0
    greaterOrEqual.evaluate("5", "20", t) shouldBe 1.0
  }

  it should "always return 0.0 if the source number is greater than the target number" in {
    greater.evaluate("2", "1", t) shouldBe 0.0
    greater.evaluate("0.223", "0.123", t) shouldBe 0.0
    greater.evaluate("20", "5", t) shouldBe 0.0
    greaterOrEqual.evaluate("2", "1", t) shouldBe 0.0
    greaterOrEqual.evaluate("0.2", "0.1", t) shouldBe 0.0
    greaterOrEqual.evaluate("20", "5", t) shouldBe 0.0
  }

  it should "return 1.0 if the source number is equal to the target number, provided that orEqual is false" in {
    greater.evaluate("2", "2", t) shouldBe 1.0
    greater.evaluate("0.123", "0.123", t) shouldBe 1.0
    greater.evaluate("20", "20", t) shouldBe 1.0
  }

  it should "return 0.0 if the source number is equal to the target number, provided that orEqual is true" in {
    greaterOrEqual.evaluate("2", "2", t) shouldBe 0.0
    greaterOrEqual.evaluate("0.123", "0.123", t) shouldBe 0.0
    greaterOrEqual.evaluate("20", "20", t) shouldBe 0.0
  }

  it should "always return 1.0 if the source string is lower than the target string" in {
    greater.evaluate("aaa", "aab", t) shouldBe 1.0
    greaterOrEqual.evaluate("aaa", "aab", t) shouldBe 1.0
  }

  it should "always return 0.0 if the source string is higher than the target string" in {
    greater.evaluate("aab", "aaa", t) shouldBe 0.0
    greaterOrEqual.evaluate("aab", "aaa", t) shouldBe 0.0
  }

  it should "return 1.0 if the source string is equal to the target number, provided that orEqual is false" in {
    greater.evaluate("xx-55", "xx-55", t) shouldBe 1.0
  }

  it should "return 0.0 if the source string is equal to the target number, provided that orEqual is true" in {
    greaterOrEqual.evaluate("xx-55", "xx-55", t) shouldBe 0.0
  }

  it should "be reversible" in {
    greaterReversed.apply(Seq("aab"), Seq("aaa"), t) shouldBe 1.0
    greaterReversed.apply(Seq("aaa"), Seq("aab"), t) shouldBe 0.0
  }

  override def pluginObject: GreaterThanMetric = greater
}