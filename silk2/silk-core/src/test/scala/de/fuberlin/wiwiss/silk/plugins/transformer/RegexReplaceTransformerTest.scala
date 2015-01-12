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
import de.fuberlin.wiwiss.silk.plugins.transformer.replace.RegexReplaceTransformer

@RunWith(classOf[JUnitRunner])
class RegexReplaceTransformerTest extends FlatSpec with ShouldMatchers {
  CorePlugins.register()

  val transformer = new RegexReplaceTransformer(regex = "[^0-9]*", replace = "")

  "RegexReplaceTransformerTest" should "return 'abc'" in {
    transformer.evaluate("a0b1c2") should equal("012")
  }

  val transformer1 = new RegexReplaceTransformer(regex = "[a-z]*", replace = "")

  "RegexReplaceTransformerTest" should "return '1'" in {
    transformer1.evaluate("abcdef1") should equal("1")
  }
}