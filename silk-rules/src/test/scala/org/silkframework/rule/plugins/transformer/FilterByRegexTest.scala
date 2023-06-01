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

package org.silkframework.rule.plugins.transformer

import org.silkframework.rule.plugins.transformer.filter.FilterByRegex
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class FilterByRegexTest extends AnyFlatSpec with Matchers {
  // Filters all strings consisting of three letters
  val threeLettersFilter = new FilterByRegex(regex = "\\w\\w\\w")

  "FilterByRegex(regex = '\\w\\w\\w')" should "only retain strings consisting of three letters" in {
    threeLettersFilter(Seq(Seq("abc"))) should equal(Seq("abc"))
    threeLettersFilter(Seq(Seq("ab"))) should equal(Seq())
    threeLettersFilter(Seq(Seq("abcd"))) should equal(Seq())
    threeLettersFilter(Seq(Seq("a-x"))) should equal(Seq())
    threeLettersFilter(Seq(Seq("ab", "abc", "abcd"))) should equal(Seq("abc"))
  }
}