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
import org.silkframework.plugins.transformer.substring.StripPostfixTransformer


class StripPostfixTransformerTest extends FlatSpec with Matchers {

  val transformer = new StripPostfixTransformer(postfix = "abc")

  "StripPostfixTransfomer" should "return 'abc123'" in {
    transformer.evaluate("abc123") should equal("abc123")
  }

  val transformer1 = new StripPostfixTransformer(postfix = "123")

  "StripPostfixTransfomer" should "return 'abc'" in {
    transformer1.evaluate("abc123") should equal("abc")
  }
}