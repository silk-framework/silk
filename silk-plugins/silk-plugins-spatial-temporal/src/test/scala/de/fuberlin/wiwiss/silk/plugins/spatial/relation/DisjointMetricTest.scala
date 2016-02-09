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

package org.silkframework.plugins.spatial.relation

import org.scalatest.{FlatSpec, Matchers}



/**
 * Tests the Disjoint Metric.
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */


class DisjointMetricTest extends FlatSpec with Matchers {

  val metric = new DisjointMetric()

  //Disjoint evaluation.
  "DisjointMetric test 1" should "return '0.0'" in {
    metric.evaluate("POLYGON ((0 0, 0 2, 2 2, 2 0, 0 0))", "POINT (3 3)", 0.0) should equal(0.0)
  }

}
