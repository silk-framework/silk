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

package org.silkframework.testutil

import org.scalatest.matchers.{BeMatcher, MatchResult}

/**
 * Matcher to test if 2 values are approximately equal.
 */
case class approximatelyEqualToOption(r: Option[Double]) extends BeMatcher[Option[Double]] {
  val epsilon = 0.001

  def apply(l: Option[Double]) = (r,l) match {
    case (Some(x), Some(y)) => approximatelyEqualTo(x)(y)
    case _ => {
      MatchResult(
        l.isDefined == r.isDefined,
        l.toString + " is not approximately equal to " + r,
        l.toString + " is approximately equal to " + r
      )
    }
  }
}