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


class GeographicDistanceMetricTest extends FlatSpec with Matchers {

  "GeographicDistanceMetric" should "return 0.0 if the coordinates are equal" in {
    val metric = new GeographicDistanceMetric()

    metric.evaluate("37.807981 -122.264609", "37.807981 -122.264609") should be(approximatelyEqualTo(0.0))
    metric.evaluate("POINT(-0.124722 51.5081)", "POINT(-0.124722 51.5081)") should be(approximatelyEqualTo(0.0))
  }

  "GeographicDistanceMetric" should "return the distance of London and Berlin in kilometers" in {
    val metric = new GeographicDistanceMetric("km")

    metric.evaluate("POINT(-0.1167 51.5000)", "POINT(13.4000 52.5167)") should be(approximatelyEqualTo(930.60))
  }

  "GeographicDistanceMetric" should "return the distance of London and Berlin in meters" in {
    val metric = new GeographicDistanceMetric("m")

    metric.evaluate("POINT(-0.1167 51.5000)", "POINT(13.4000 52.5167)") should be(approximatelyEqualTo(930600.26))
  }
}