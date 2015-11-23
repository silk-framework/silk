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

package org.silkframework.plugins.temporal.distance

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * Tests the YearsDistance Metric.
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */

@RunWith(classOf[JUnitRunner])
class YearsDistanceMetricTest extends FlatSpec with Matchers {

  val metric = new YearsDistanceMetric()

  //1 Year Distance.
  "YearsDistanceMetric test 1" should "return '1.0'" in {
    metric.evaluate("2000-01-01T00:00:00", "2001-01-01T00:00:00") should equal(1.0)
  }
}
