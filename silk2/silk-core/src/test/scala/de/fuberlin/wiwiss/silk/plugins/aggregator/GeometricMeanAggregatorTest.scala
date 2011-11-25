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

package de.fuberlin.wiwiss.silk.plugins.aggregator

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.plugins.aggegrator.GeometricMeanAggregator
import de.fuberlin.wiwiss.silk.plugins.util.approximatelyEqualTo

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GeometricMeanAggregatorTest extends FlatSpec with ShouldMatchers {
  val aggregator = new GeometricMeanAggregator()

  "GeometricMeanAggregator" should "compute the weighted geometric mean" in {
    aggregator.evaluate((1, 0.0) :: (2, 0.0) :: (1, 0.0) :: Nil).get should be(approximatelyEqualTo(0.0))
    aggregator.evaluate((1, 1.0) :: (2, 1.0) :: (1, 1.0) :: Nil).get should be(approximatelyEqualTo(1.0))
    aggregator.evaluate((2, 0.5) :: (1, 1.0) :: Nil).get should be(approximatelyEqualTo(0.629961))
    aggregator.evaluate((2, 0.5) :: (1, 1.0) :: (5, 0.7) :: Nil).get should be(approximatelyEqualTo(0.672866))
    aggregator.evaluate((10, 0.1) :: (2, 0.9) :: (3, 0.2) :: Nil).get should be(approximatelyEqualTo(0.153971))
  }
}