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

package de.fuberlin.wiwiss.silk.plugins.metric.asian

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import de.fuberlin.wiwiss.silk.plugins.metric.KoreanTranslitDistance

@RunWith(classOf[JUnitRunner])
class KoreanTranslitDistanceTest extends FlatSpec with ShouldMatchers {

  val metric = new KoreanTranslitDistance()

  "KoreanTranslitDistance" should "return distance 0 for equal strings" in {
    metric.evaluate("shinhanbank", "sinhanbank") should equal(0)
    metric.evaluate("dotorimook", "dotorimoog", 0.0) should equal(0)
  }

  "KoreanTranslitDistance" should "return distance 1 (달, 돌)" in {
    metric.evaluate("haneul", "ganeul") should equal(1)
  }
}