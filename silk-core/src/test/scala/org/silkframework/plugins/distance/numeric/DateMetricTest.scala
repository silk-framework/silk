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

class DateMetricTest extends FlatSpec with Matchers {
  val metric = new DateMetric()

  "DateMetric" should "return the distance in days" in {
    metric.evaluate("2003-03-01", "2003-03-01") should be(approximatelyEqualTo(0.0))
    metric.evaluate("2003-03-01", "2003-03-02") should be(approximatelyEqualTo(1.0))
    metric.evaluate("2003-03-01", "2003-04-01") should be(approximatelyEqualTo(31.0))
    metric.evaluate("2003-03-01", "2003-04-02") should be(approximatelyEqualTo(32.0))
  }

  "DateMetric" should "ignore the time of day" in {
    metric.evaluate("2010-09-24", "2010-09-30") should be(approximatelyEqualTo(6.0))
    metric.evaluate("2010-09-24T06:00:00", "2010-09-30T06:00:00") should be(approximatelyEqualTo(6.0))
  }
}
