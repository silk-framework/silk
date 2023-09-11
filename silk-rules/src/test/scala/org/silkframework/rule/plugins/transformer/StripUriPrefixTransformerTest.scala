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

import org.silkframework.rule.plugins.transformer.substring.StripUriPrefixTransformer
import org.silkframework.rule.test.TransformerTest
import org.silkframework.test.PluginTest

class StripUriPrefixTransformerTest extends TransformerTest[StripUriPrefixTransformer] {

  private def transformer = pluginObject

  "StripUriPrefixTransformer" should "return 'Apple'" in {
    transformer.evaluate("http://dbpedia.org/resource/Apple") should equal("Apple")
  }

  "StripUriPrefixTransformer" should "return 'Moon'" in {
    transformer.evaluate("http://dbpedia.org/resource#Moon") should equal("Moon")
  }

}
