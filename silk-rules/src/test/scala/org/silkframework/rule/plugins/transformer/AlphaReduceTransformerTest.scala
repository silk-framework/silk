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

import org.silkframework.rule.plugins.transformer.normalize.AlphaReduceTransformer
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class AlphaReduceTransformerTest extends AnyFlatSpec with Matchers {

  val transformer = new AlphaReduceTransformer()

  "AlphaReduceTransformer" should "remove numbers" in {
    transformer.evaluate("a1b0c") should equal("abc")
  }

  "AlphaReduceTransformer" should "remove punctuation, but retain spaces" in {
    transformer.evaluate("-.def ,-") should equal("def ")
  }
}