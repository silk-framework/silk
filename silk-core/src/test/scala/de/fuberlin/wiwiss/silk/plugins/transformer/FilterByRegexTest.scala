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

package de.fuberlin.wiwiss.silk.plugins.transformer

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.plugins.CorePlugins
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import de.fuberlin.wiwiss.silk.plugins.transformer.filter.FilterByRegex

@RunWith(classOf[JUnitRunner])
class FilterByRegexTest extends FlatSpec with ShouldMatchers {
  CorePlugins.register()

  // Filters all strings consisting of three letters
  val threeLettersFilter = new FilterByRegex(regex = "\\w\\w\\w")

  "FilterByRegex(regex = '\\w\\w\\w')" should "only retain strings consisting of three letters" in {
    threeLettersFilter(Seq(Set("abc"))) should equal(Set("abc"))
    threeLettersFilter(Seq(Set("ab"))) should equal(Set())
    threeLettersFilter(Seq(Set("abcd"))) should equal(Set())
    threeLettersFilter(Seq(Set("a-x"))) should equal(Set())
    threeLettersFilter(Seq(Set("ab", "abc", "abcd"))) should equal(Set("abc"))
  }
}