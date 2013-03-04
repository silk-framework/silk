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
class DateTimeMetricTest extends FlatSpec with ShouldMatchers {
  val metric = new DateTimeMetric()

  "DateTimeMetric" should "return 0.0 for equal values" in {
    metric.evaluate("2010-09-24T05:00:00", "2010-09-24T05:00:00") should be(approximatelyEqualTo(0.0))
  }

  "DateTimeMetric" should "return the correct similarity" in {
    metric.evaluate("2001-10-26T21:32:10", "2001-10-26T21:32:40") should be(approximatelyEqualTo(30.0))
  }
}