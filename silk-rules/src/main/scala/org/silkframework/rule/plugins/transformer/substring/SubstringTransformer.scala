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

package org.silkframework.rule.plugins.transformer.substring

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, TransformExample, TransformExamples}

@Plugin(
  id = "substring",
  categories = Array("Substring"),
  label = "Substring",
  description =
"""Returns a substring between 'beginIndex' (inclusive) and 'endIndex' (exclusive).
If 'endIndex' is 0 (default), it is ignored and the entire remaining string starting with 'beginIndex' is returned.
If 'endIndex' is negative, -endIndex characters are removed from the end."""
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array("beginIndex", "0", "endIndex", "1"),
    input1 = Array("abc"),
    output = Array("a")
  ),
  new TransformExample(
    parameters = Array("beginIndex", "2", "endIndex", "3"),
    input1 = Array("abc"),
    output = Array("c")
  ),
  new TransformExample(
    parameters = Array("beginIndex", "2", "endIndex", "4", "stringMustBeInRange", "false"),
    input1 = Array("abc"),
    output = Array("c")
  ),
  new TransformExample(
    parameters = Array("beginIndex", "0", "endIndex", "-1"),
    input1 = Array("abc"),
    output = Array("ab")
  ),
  new TransformExample(
    parameters = Array("beginIndex", "1", "endIndex", "0"),
    input1 = Array("abc"),
    output = Array("bc")
  )
))
case class SubstringTransformer(
  @Param("The beginning index, inclusive.")
  beginIndex: Int = 0,
  @Param("The end index, exclusive. Ignored if set to 0, i.e., the entire remaining string starting with 'beginIndex' is returned. If negative, -endIndex characters are removed from the end ")
  endIndex: Int = 0,
  @Param("If true, only strings will be accepted that are within the start and end indices.")
  stringMustBeInRange: Boolean = true) extends SimpleTransformer {

  override def evaluate(value: String): String = {
    var start = beginIndex
    var end = endIndex

    // Handle negative indices
    if(end < 0) {
      end += value.length
    }
    if(start < 0) {
      start += value.length
    }

    // Check if end index is within range
    if(!stringMustBeInRange && end > value.length) {
      end = value.length
    }

    if(endIndex == 0) {
      value.substring(start)
    } else {
      value.substring(start, end)
    }
  }
}
