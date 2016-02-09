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

package org.silkframework.plugins.temporal.relation

import org.scalatest.Matchers
import org.scalatest.FlatSpec



/**
 * Tests the After Metric.
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */


class AfterMetricTest extends FlatSpec with Matchers {

  val metric = new AfterMetric()

  "AfterMetric" should "compare date time intervals" in {
    metric.evaluate("[2000-01-01T00:00:02, 2000-01-01T00:00:03)", "[2000-01-01T00:00:00, 2000-01-01T00:00:01)") should equal(0.0)
  }

  "AfterMetric" should "compare date times" in {
    metric.evaluate("2000-01-05T00:00:00", "2000-01-01T00:00:00") should equal(0.0)
    metric.evaluate("2000-01-01T00:00:00", "2000-01-02T00:00:00") should equal(1.0)
    metric.evaluate("2000-01-01T00:00:01", "2000-01-01T00:00:00") should equal(0.0)
    metric.evaluate("2000-01-01T00:00:00", "2000-01-01T00:00:01") should equal(1.0)
  }

  "AfterMetric" should "compare dates" in {
    metric.evaluate("2000-01-05", "2000-01-01") should equal(0.0)
    metric.evaluate("2000-01-00", "2000-01-02") should equal(1.0)
  }
}
