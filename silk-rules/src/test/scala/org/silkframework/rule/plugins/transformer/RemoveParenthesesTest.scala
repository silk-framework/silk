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

import org.silkframework.rule.plugins.transformer.normalize.RemoveParentheses
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class RemoveParenthesesTest extends AnyFlatSpec with Matchers {

  val transformer = new RemoveParentheses()

  "RemoveParentheses" should "remove parentheses including their contents" in {
    transformer.evaluate("Berlin(city)") should equal("Berlin")
    transformer.evaluate("(city)Berlin") should equal("Berlin")
    transformer.evaluate("Berlin(city)Berlin") should equal("BerlinBerlin")
  }

  "RemoveParentheses" should "remove whitespaces before and after parentheses" in {
    transformer.evaluate("Berlin (city)") should equal("Berlin")
    transformer.evaluate("Berlin   (city)") should equal("Berlin")
    transformer.evaluate("Berlin (city)  Berlin") should equal("BerlinBerlin")
  }
}