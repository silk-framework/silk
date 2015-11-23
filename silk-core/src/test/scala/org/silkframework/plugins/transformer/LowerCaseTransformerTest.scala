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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.silkframework.plugins.CorePlugins
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.silkframework.plugins.transformer.normalize.LowerCaseTransformer

@RunWith(classOf[JUnitRunner])
class LowerCaseTransformerTest extends FlatSpec with ShouldMatchers {

  val transformer = new LowerCaseTransformer()

  "LowerCaseTransformer" should "return '123'" in {
    transformer.evaluate("123") should equal("123")
  }

  val transformer1 = new LowerCaseTransformer()

  "LowerCaseTransformer" should "return 'abc'" in {
    transformer1.evaluate("ABc") should equal("abc")
  }
}