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

package de.fuberlin.wiwiss.silk.plugins.distance.tokenbased

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import de.fuberlin.wiwiss.silk.testutil.approximatelyEqualTo

@RunWith(classOf[JUnitRunner])
class DiceCoefficientTest extends FlatSpec with ShouldMatchers {
  val distance = new DiceCoefficient()

  "DiceCoefficient" should "return dice coefficient" in {
    distance("A" :: "B" :: Nil, "C" :: "D" :: Nil) should be(approximatelyEqualTo(1.0))
    distance("Same" :: "Different1" :: Nil, "Same" :: "Different2" :: Nil) should be(approximatelyEqualTo(0.5))
    distance("A" :: "B" :: "C" :: Nil, "A" :: "B" :: "D" :: Nil) should be(approximatelyEqualTo(1.0 / 3.0))
    distance("A" :: "B" :: "C" :: Nil, "A" :: "B" :: "C" :: Nil) should be(approximatelyEqualTo(0.0))
  }

//  "DiceCoefficient" should "index one values if the limit is 0" in {
//    distance.index(Set("A", "B", "C"), 0.0).size should equal (1)
//  }
//
//  "DiceCoefficient" should "index all values if the limit is 1" in {
//    distance.index(Set("A", "B", "C"), 1.0).size should equal (3)
//  }
//
//  "DiceCoefficient" should "index the minimum number of values" in {
//    distance.index(Set("A"), 0.5).size should equal (1)
//    distance.index(Set("A", "B"), 0.5).size should equal (2)
//    distance.index(Set("A", "B", "C"), 0.5).size should equal (3)
//    distance.index(Set("A", "B", "C", "D"), 0.5).size should equal (3)
//    distance.index(Set("A", "B", "C", "D", "E"), 0.5).size should equal (4)
//    distance.index(Set("A", "B", "C", "D", "E", "F"), 0.5).size should equal (5)
//    distance.index(Set("A"), 0.2).size should equal (1)
//    distance.index(Set("A", "B"), 0.2).size should equal (1)
//    distance.index(Set("A", "B", "C"), 0.2).size should equal (2)
//    distance.index(Set("A", "B", "C", "D"), 0.2).size should equal (2)
//    distance.index(Set("A", "B", "C", "D", "E"), 0.2).size should equal (2)
//    distance.index(Set("A", "B", "C", "D", "E", "F"), 0.2).size should equal (3)
//  }
}