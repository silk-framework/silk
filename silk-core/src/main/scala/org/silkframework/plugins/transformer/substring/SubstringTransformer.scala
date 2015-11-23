
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

package org.silkframework.plugins.transformer.substring

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin

/**
 * Returns a substring between 'beginIndex' (inclusive) and 'endIndex' (exclusive).
 *
 * @author Robert Isele
 */
@Plugin(
  id = "substring",
  categories = Array("Substring"),
  label = "Substring",
  description =
"""Returns a substring between 'beginIndex' (inclusive) and 'endIndex' (exclusive).
If 'endIndex' is 0 (default), it is ignored and the entire remaining string starting with 'beginIndex' is returned.
If 'endIndex' is negative, -endIndex characters are removed from the end.'"""
)
case class SubstringTransformer(beginIndex: Int = 0, endIndex: Int = 0) extends SimpleTransformer {
  override def evaluate(value: String) = {
    if(endIndex > 0)
      value.substring(beginIndex, endIndex)
    else if(endIndex == 0)
      value.substring(beginIndex)
    else if(value.length > -endIndex)
      value.substring(beginIndex, value.length + endIndex)
    else
      ""
  }
}
