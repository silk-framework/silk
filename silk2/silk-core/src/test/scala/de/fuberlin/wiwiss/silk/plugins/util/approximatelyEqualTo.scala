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

package de.fuberlin.wiwiss.silk.plugins.util

import org.scalatest.matchers.{MatchResult, BeMatcher}

/**
 * Matcher to test if 2 values are approximately equal.
 */
case class approximatelyEqualTo(r: Double) extends BeMatcher[Double] {
  val epsilon = 0.001

  def apply(l: Double) =
    MatchResult(
      compare(l, r),
      l + " is not approximately equal to " + r,
      l + " is approximately equal to " + r
    )

  private def compare(l: Double, r: Double): Boolean = {
    math.abs(l - r) < epsilon
  }
}