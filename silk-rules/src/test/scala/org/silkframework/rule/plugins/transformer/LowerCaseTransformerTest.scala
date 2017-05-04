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

import org.silkframework.rule.plugins.transformer.normalize.LowerCaseTransformer
import org.silkframework.test.PluginTest

class LowerCaseTransformerTest extends PluginTest {

  private val transformer = pluginObject

  "LowerCaseTransformer" should "return '123'" in {
    transformer.evaluate("123") should equal("123")
  }

  "LowerCaseTransformer" should "return 'abc'" in {
    transformer.evaluate("ABc") should equal("abc")
  }

  override protected def pluginObject = LowerCaseTransformer()
}