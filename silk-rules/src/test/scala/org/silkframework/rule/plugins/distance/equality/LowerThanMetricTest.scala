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

class LowerThanMetricTest extends PluginTest {

  lazy val lower = LowerThanMetric()
  lazy val lowerOrEqual = LowerThanMetric(orEqual = true)
  val t = 1.0

  behavior of "LowerThanMetric"

  it should "always return 0.0 if the source number is smaller than the target number" in {
    lower.evaluate("1", "2", t) shouldBe 0.0
    lower.evaluate("0.123", "0.223", t) shouldBe 0.0
    lower.evaluate("5", "20", t) shouldBe 0.0
    lowerOrEqual.evaluate("1", "2", t) shouldBe 0.0
    lowerOrEqual.evaluate("0.1", "0.2", t) shouldBe 0.0
    lowerOrEqual.evaluate("5", "20", t) shouldBe 0.0
  }

  it should "always return 1.0 if the source number is greater than the target number" in {
    lower.evaluate("2", "1", t) shouldBe 1.0
    lower.evaluate("0.223", "0.123", t) shouldBe 1.0
    lower.evaluate("20", "5", t) shouldBe 1.0
    lowerOrEqual.evaluate("2", "1", t) shouldBe 1.0
    lowerOrEqual.evaluate("0.2", "0.1", t) shouldBe 1.0
    lowerOrEqual.evaluate("20", "5", t) shouldBe 1.0
  }

  it should "return 1.0 if the source number is equal to the target number, provided that orEqual is false" in {
    lower.evaluate("2", "2", t) shouldBe 1.0
    lower.evaluate("0.123", "0.123", t) shouldBe 1.0
    lower.evaluate("20", "20", t) shouldBe 1.0
  }

  it should "return 0.0 if the source number is equal to the target number, provided that orEqual is true" in {
    lowerOrEqual.evaluate("2", "2", t) shouldBe 0.0
    lowerOrEqual.evaluate("0.123", "0.123", t) shouldBe 0.0
    lowerOrEqual.evaluate("20", "20", t) shouldBe 0.0
  }

  it should "always return 0.0 if the source string is lower than the target string" in {
    lower.evaluate("aaa", "aab", t) shouldBe 0.0
    lowerOrEqual.evaluate("aaa", "aab", t) shouldBe 0.0
  }

  it should "always return 1.0 if the source string is higher than the target string" in {
    lower.evaluate("aab", "aaa", t) shouldBe 1.0
    lowerOrEqual.evaluate("aab", "aaa", t) shouldBe 1.0
  }

  it should "return 1.0 if the source string is equal to the target number, provided that orEqual is false" in {
    lower.evaluate("xx-55", "xx-55", t) shouldBe 1.0
  }

  it should "return 0.0 if the source string is equal to the target number, provided that orEqual is true" in {
    lowerOrEqual.evaluate("xx-55", "xx-55", t) shouldBe 0.0
  }

  override def pluginObject: LowerThanMetric = lower
}