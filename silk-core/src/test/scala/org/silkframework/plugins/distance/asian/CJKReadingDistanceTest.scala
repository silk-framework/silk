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

package org.silkframework.plugins.distance.asian

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class CJKReadingDistanceTest extends FlatSpec with ShouldMatchers {

  val metric = new CJKReadingDistance()

  "CJKReadingDistance" should "return distance 0 for equal strings" in {
    metric.evaluate("贾逵", "贾逵") should equal(0)
    metric.evaluate("川島芳子", "川島芳子", 0.0) should equal(0)
  }

  "CJKReadingDistance" should "return distance 2 (祚, 胙)" in {
    metric.evaluate("祚", "胙") should equal(2)
    metric.evaluate("賈逵", "贾岛") should equal(9)
    metric.evaluate("賈逵", "賈範", 5) should equal(5)
    metric.evaluate("賈似道", "賈南風", 10) should equal(10)
  }
}