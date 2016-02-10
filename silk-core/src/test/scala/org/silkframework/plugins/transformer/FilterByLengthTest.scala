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

package org.silkframework.plugins.transformer

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.plugins.transformer.filter.FilterByLength


class FilterByLengthTest extends FlatSpec with Matchers {
  val transformer = new FilterByLength(min = 3, max = 5)

  "FilterByLength" should "retain strings inside the range" in {
    transformer(Seq(Seq("abc"))) should equal(Seq("abc"))
    transformer(Seq(Seq("abcd"))) should equal(Seq("abcd"))
    transformer(Seq(Seq("abcde"))) should equal(Seq("abcde"))
  }

  "FilterByLength" should "remove strings outside the range" in {
    transformer(Seq(Seq("ab"))) should equal(Seq())
    transformer(Seq(Seq("abc", "ab", "abd", "abcdef"))) should equal(Seq("abc", "abd"))
  }
}