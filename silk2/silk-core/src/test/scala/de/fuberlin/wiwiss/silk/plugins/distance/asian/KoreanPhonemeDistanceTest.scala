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

package de.fuberlin.wiwiss.silk.plugins.distance.asian

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

//@RunWith(classOf[JUnitRunner])
class KoreanPhonemeDistanceTest extends FlatSpec with ShouldMatchers {

  val metric = new KoreanPhonemeDistance()

  "KoreanPhonemeDistance" should "return distance 0 for equal strings" in {
    metric.evaluate("한글", "한글") should equal(0)
    metric.evaluate("세종대왕", "세종대왕", 0.0) should equal(0)
  }

  "KoreanPhonemeDistance" should "return distance 1 (달, 돌)" in {
    metric.evaluate("달", "돌") should equal(1)
    metric.evaluate("국수", "국시") should equal(1)
    metric.evaluate("도토리묵", "도토리묵무침", 5) should equal(5)
    metric.evaluate("송편", "절편", 3) should equal(3)
  }
}