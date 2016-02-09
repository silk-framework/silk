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

package org.silkframework.plugins.transformer.conversion

import java.nio.charset.Charset

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin

@Plugin(
  id = "convertCharset",
  categories = Array("Conversion"),
  label = "Convert Charset",
  description = "Convert the string from \"sourceCharset\" to \"targetCharset\"."
)
case class ConvertCharsetTransformer(sourceCharset: String = "ISO-8859-1", targetCharset: String = "UTF-8") extends SimpleTransformer {
  require(Charset.isSupported(sourceCharset), "sourceCharset " + sourceCharset + " is unsupported")
  require(Charset.isSupported(targetCharset), "targetCharset " + targetCharset + " is unsupported")

  override def evaluate(value: String) = {
    val bytes = value.getBytes(sourceCharset)
    new String(bytes, targetCharset)
  }
}