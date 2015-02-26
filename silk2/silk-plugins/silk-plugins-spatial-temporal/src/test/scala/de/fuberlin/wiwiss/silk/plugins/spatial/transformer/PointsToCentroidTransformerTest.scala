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

package de.fuberlin.wiwiss.silk.plugins.spatial.transformer

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * Tests the PointsToCentroid Transformer.
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */

@RunWith(classOf[JUnitRunner])
class PointsToCentroidTransformerTest extends FlatSpec with Matchers {

  val transformer = new PointsToCentroidTransformer()

  //Centroid of 2 Points.
  "PointsToCentroidTransformer test 1" should "return 'Set(\"POINT (2.0 2.0)\")'" in {
    transformer.apply(Seq(Set("1", "3"), Set("1", "3"))) should equal(Set("POINT (2.0 2.0)"))
  }  
}
