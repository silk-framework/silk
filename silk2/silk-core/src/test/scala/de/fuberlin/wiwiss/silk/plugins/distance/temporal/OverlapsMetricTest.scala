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

package de.fuberlin.wiwiss.silk.plugins.distance.temporal

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.plugins.Plugins
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * Tests the Overlaps Metric.
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */

@RunWith(classOf[JUnitRunner])
class OverlapsMetricTest extends FlatSpec with ShouldMatchers {
  Plugins.register()

  val metric = new OverlapsMetric()

  //Overlaps evaluation.
  "OverlapsMetric test 1" should "return '1.0'" in {
    metric.evaluate("[2000-01-01T00:00:00, 2000-01-01T00:00:02)", "[2000-01-01T00:00:01, 2000-01-01T00:00:03)", 1.0) should equal(1.0)
  }
}
