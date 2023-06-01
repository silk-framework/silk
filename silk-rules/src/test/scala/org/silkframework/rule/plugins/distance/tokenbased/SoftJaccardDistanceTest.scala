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

package org.silkframework.rule.plugins.distance.tokenbased



import org.silkframework.test.PluginTest
import org.silkframework.testutil.approximatelyEqualTo


class SoftJaccardDistanceTest extends PluginTest {

  lazy val distance = SoftJaccardDistance(maxDistance = 1)

  "SoftJaccardDistance" should "return soft jaccard distance" in {
    distance("AA" :: "BB" :: Nil, "CC" :: "DD" :: Nil) should be(approximatelyEqualTo(1.0))
    distance("AA" :: "BB" :: "C" :: Nil, "AA" :: "B" :: "CC" :: Nil) should be(approximatelyEqualTo(0.0))
    distance("Same1" :: "Different11" :: Nil, "Same2" :: "Different22" :: Nil) should be(approximatelyEqualTo(2.0 / 3.0))
    distance("A1" :: "B1" :: "CC" :: Nil, "A2" :: "B2" :: "DD" :: Nil) should be(approximatelyEqualTo(0.5))
  }

  override def pluginObject = distance

}