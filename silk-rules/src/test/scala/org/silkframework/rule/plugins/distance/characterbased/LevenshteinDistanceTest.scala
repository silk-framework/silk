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

package org.silkframework.rule.plugins.distance.characterbased

import org.silkframework.entity.Index
import org.silkframework.rule.test.DistanceMeasureTest

class LevenshteinDistanceTest extends DistanceMeasureTest[LevenshteinDistance] {

  lazy val metric = new LevenshteinDistance()

  it should "return distance 0 for equal strings" in {
    metric.evaluate("kitten", "kitten") should equal(0)
    metric.evaluate("sitting", "sitting", 0.0) should equal(0)
  }

  it should "return distance 3 (kitten, sitting)" in {
    metric.evaluate("kitten", "sitting") should equal(3)
    metric.evaluate("sitting", "kitten") should equal(3)
    metric.evaluate("kitten", "sitting", 3.0) should equal(3)
    metric.evaluate("sitting", "kitten", 3.0) should equal(3)
  }

  it should "index correctly" in {
    (metric.indexValue("Sunday", 3, sourceOrTarget = false) matches metric.indexValue("Saturday", 2, sourceOrTarget = false)) should equal(false)
    (metric.indexValue("Sunday", 3, sourceOrTarget = false) matches metric.indexValue("Saturday", 3, sourceOrTarget = false)) should equal(true)
  }

  it should "generate the default index, if indexing is disabled" in {
    LevenshteinDistance(qGramsSize = 1).indexValue("test", 1.0, sourceOrTarget = false) should not equal Index.default
    LevenshteinDistance(qGramsSize = 0).indexValue("test", 1.0, sourceOrTarget = false) shouldBe Index.default
  }
}