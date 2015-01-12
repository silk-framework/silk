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

package de.fuberlin.wiwiss.silk.plugins.distance.spatial

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import de.fuberlin.wiwiss.silk.entity.Index

/**
 * Tests the CentroidDistance Metric.
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */

@RunWith(classOf[JUnitRunner])
class CentroidDistanceMetricTest extends FlatSpec with Matchers {

  val metric = new CentroidDistanceMetric()

  //Same point.
  "CentroidDinstaceMetric test 1" should "return '0.0'" in {
    metric.evaluate("POINT (0 0)", "POINT (0 0)") should equal(0.0)
  }

  //Same centroid.
  "CentroidDinstaceMetric test 2" should "return '0.0'" in {
    metric.evaluate("POLYGON ((0 0, 0 2, 2 2, 2 0, 0 0))", "POINT (1 1)") should equal(0.0)

  }

  //Big threshold.
  "CentroidDinstaceMetric test 3" should "not return 'Double.PositiveInfinity'" in {
    metric.evaluate("POINT (37.9889023 23.7180747)", "POINT (37.9884826 23.7181476)", 100.0) should not equal(Double.PositiveInfinity)
  }

  //Small threshold.
  "CentroidDinstaceMetric test 4" should "return 'Double.PositiveInfinity'" in {
    metric.evaluate("POINT (37.9889023 23.7180747)", "POINT (37.9884826 23.7181476)", 10.0) should equal(Double.PositiveInfinity)
  }
  
  //Indexing.
  "CentroidDinstaceMetric test 5" should "return '(Set(List(0, 0))'" in {
    metric.indexValue("POINT (0 0)", 0.0) should equal(Index.multiDim(Set(Seq(0, 0)), 2))
  }  
}
