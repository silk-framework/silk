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

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}
import org.silkframework.runtime.validation.ValidationException

@Plugin(
  id = SubstringTransformer.pluginId,
  categories = Array("Substring"),
  label = "Substring",
  description = """Returns a substring between 'beginIndex' (inclusive) and 'endIndex' (exclusive). If 'endIndex' is 0 (default), it is ignored and the entire remaining string starting with 'beginIndex' is returned. If 'endIndex' is negative, -endIndex characters are removed from the end.""",
  relatedPlugins = Array(
    new PluginReference(
      id = StripPrefixTransformer.pluginId,
      description = "Substring removes a fixed number of characters from the start regardless of their content. Strip prefix is more selective: it only removes from the start if the configured string is actually found there."
    ),
    new PluginReference(
      id = StripPostfixTransformer.pluginId,
      description = "Substring works by index: it removes a fixed count of trailing characters regardless of their content. Strip postfix is the alternative when the trailing portion is a known string; it checks for it and leaves the value unchanged if not found."
    ),
    new PluginReference(
      id = UntilCharacterTransformer.pluginId,
      description = "Substring extracts by position: the start and end indices are fixed and apply to every input value regardless of its content. Until character extracts up to a specific character."
    )
  )
)
@TransformExamples(Array(
  new TransformExample(
    description = "Returns the substring between 'beginIndex' (inclusive) and 'endIndex' (exclusive). Indices are zero-based.",
    parameters = Array("beginIndex", "0", "endIndex", "1"),
    input1 = Array("abc"),
    output = Array("a")
  ),
  new TransformExample(
    description = "Extracts a single character if 'endIndex' is 'beginIndex' plus one.",
    parameters = Array("beginIndex", "2", "endIndex", "3"),
    input1 = Array("abc"),
    output = Array("c")
  ),
  new TransformExample(
    description = "Equal indices yield an empty string.",
    parameters = Array("beginIndex", "3", "endIndex", "3"),
    input1 = Array("abc"),
    output = Array("")
  ),
  new TransformExample(
    description = "Indices that are out of range for the value are rejected by default.",
    parameters = Array("beginIndex", "2", "endIndex", "4"),
    input1 = Array("abc"),
    throwsException = classOf[org.silkframework.runtime.validation.ValidationException]
  ),
  new TransformExample(
    description = "If 'stringMustBeInRange' is false, out-of-range indices are clipped to the length of the value.",
    parameters = Array("beginIndex", "2", "endIndex", "4", "stringMustBeInRange", "false"),
    input1 = Array("abc"),
    output = Array("c")
  ),
  new TransformExample(
    description = "Clipping yields an empty string if the whole range lies outside the value.",
    parameters = Array("beginIndex", "10", "endIndex", "20", "stringMustBeInRange", "false"),
    input1 = Array("abc"),
    output = Array("")
  ),
  new TransformExample(
    description = "A negative 'endIndex' removes that many characters from the end.",
    parameters = Array("beginIndex", "0", "endIndex", "-1"),
    input1 = Array("abc"),
    output = Array("ab")
  ),
  new TransformExample(
    description = "An 'endIndex' of 0 returns the entire remaining string starting at 'beginIndex'.",
    parameters = Array("beginIndex", "1", "endIndex", "0"),
    input1 = Array("abc"),
    output = Array("bc")
  ),
  new TransformExample(
    description = "Values shorter than the number of characters to remove from the end are rejected.",
    parameters = Array("beginIndex", "0", "endIndex", "-3"),
    input1 = Array("ab"),
    throwsException = classOf[org.silkframework.runtime.validation.ValidationException]
  ),
  new TransformExample(
    description = "If 'stringMustBeInRange' is false, values shorter than the number of characters to remove yield an empty string.",
    parameters = Array("beginIndex", "0", "endIndex", "-3", "stringMustBeInRange", "false"),
    input1 = Array("ab"),
    output = Array("")
  ),
  new TransformExample(
    description = "A begin index that exceeds the end index is rejected.",
    parameters = Array("beginIndex", "2", "endIndex", "1"),
    input1 = Array("abc"),
    throwsException = classOf[org.silkframework.runtime.validation.ValidationException]
  ),
  new TransformExample(
    description = "If 'stringMustBeInRange' is false, a begin index that exceeds the end index yields an empty string.",
    parameters = Array("beginIndex", "2", "endIndex", "1", "stringMustBeInRange", "false"),
    input1 = Array("abc"),
    output = Array("")
  )
))
case class SubstringTransformer(
  @Param("The beginning index, inclusive.")
  beginIndex: Int = 0,
  @Param("The end index, exclusive. Ignored if set to 0, i.e., the entire remaining string starting with 'beginIndex' is returned. If negative, -endIndex characters are removed from the end.")
  endIndex: Int = 0,
  @Param("If true, only strings will be accepted that are within the start and end indices, throwing a validating error if an index is out of range.")
  stringMustBeInRange: Boolean = true
) extends SimpleTransformer {

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

    // Check if indexes are within range
    if(start > value.length || start < 0) {
      if(stringMustBeInRange) {
        throw new ValidationException(s"Start index $start is out of range")
      } else {
        start = if(start < 0) 0 else value.length
      }
    }
    if(end > value.length || end < 0) {
      if(stringMustBeInRange) {
        throw new ValidationException(s"End index $end is out of range")
      } else {
        end = if(end < 0) 0 else value.length
      }
    }
    // The start index may exceed the end index after negative indices have been resolved
    if(endIndex != 0 && start > end) {
      if(stringMustBeInRange) {
        throw new ValidationException(s"Start index $start exceeds end index $end")
      } else {
        end = start
      }
    }

    if(endIndex == 0) {
      value.substring(start)
    } else {
      value.substring(start, end)
    }
  }
}

object SubstringTransformer {
  final val pluginId = "substring"
}
