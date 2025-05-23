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

import org.silkframework.rule.plugins.transformer.substring.StripPrefixTransformer
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class StripPrefixTransformerTest extends AnyFlatSpec with Matchers {

  val transformer = new StripPrefixTransformer(prefix = "abc")

  "StripPrefixTransformer" should "return '123'" in {
    transformer.evaluate("abc123") should equal("123")
  }

  val transformer1 = new StripPrefixTransformer(prefix = "123")

  "StripPrefixTransformer" should "return 'abc'" in {
    transformer1.evaluate("123abc") should equal("abc")
  }
}