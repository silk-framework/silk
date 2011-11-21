/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.plugins.metric

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.plugins.util.approximatelyEqualTo

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DateMetricTest extends FlatSpec with ShouldMatchers {
  val metric = new DateMetric()
  val t = 0.9

  "DateMetric" should "not return values under 0.0" in {
    metric.evaluate("2003-03-01", "2010-09-30", 0.9) should be(approximatelyEqualTo(0.0))
  }

  "DateMetric" should "return 1.0 if the dates are equal" in {
    metric.evaluate("2010-09-30", "2010-09-30", 0.9) should be(approximatelyEqualTo(1.0))
  }

  "DateMetric" should "ignore the time of day" in {
    metric.evaluate("2010-09-24", "2010-09-30", 0.9) should be(approximatelyEqualTo(0.4))
    metric.evaluate("2010-09-24T06:00:00", "2010-09-30T06:00:00", 0.9) should be(approximatelyEqualTo(0.4))
  }
}
