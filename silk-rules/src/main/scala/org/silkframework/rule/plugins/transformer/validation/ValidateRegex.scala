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

package org.silkframework.rule.plugins.transformer.validation

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.rule.plugins.transformer.conditional.IfMatchesRegexTransformer
import org.silkframework.rule.plugins.transformer.extraction.RegexExtractionTransformer
import org.silkframework.rule.plugins.transformer.replace.RegexReplaceTransformer
import org.silkframework.rule.plugins.transformer.selection.RegexSelectTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}
import org.silkframework.runtime.validation.ValidationException

import scala.util.matching.Regex

@Plugin(
  id = ValidateRegex.pluginId,
  categories = Array("Validation"),
  label = "Validate regex",
  description = "Validates if all values match a regular expression.",
  documentationFile = "ValidateRegex.md",
  relatedPlugins = Array(
    new PluginReference(
      id = RegexReplaceTransformer.pluginId,
      description = "Regex replace rewrites the input string by substituting every match and returns the rewritten value. Validate regex treats the pattern as a full-value check on the resulting string."
    ),
    new PluginReference(
      id = IfMatchesRegexTransformer.pluginId,
      description = "A regular expression match plays different roles here. The Validate regex plugin checks each value against the pattern and passes it through only when it fully matches. The If matches regex plugin uses the match to choose which provided branch value is returned."
    ),
    new PluginReference(
      id = RegexSelectTransformer.pluginId,
      description = "Regex selection turns one checked value and a list of patterns into a result sequence aligned with that list, placing the provided output value wherever a pattern matches. Validate regex keeps the original value and treats the pattern as a full-value check."
    ),
    new PluginReference(
      id = RegexExtractionTransformer.pluginId,
      description = "Regex extract turns the match into output by returning the matched substring or the first capturing group. Validate regex leaves the value unchanged and only lets it through when the full value matches the pattern."
    )
  )
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array("regex", "\\w*"),
    input1 = Array("TestValue123"),
    output = Array("TestValue123"),
  ),
  new TransformExample(
    parameters = Array("regex", "[a-d]*"),
    input1 = Array("abcd"),
    output = Array("abcd"),
  ),
  new TransformExample(
    parameters = Array("regex", "Prefix \\w\\w\\w"),
    input1 = Array("Prefix abc"),
    output = Array("Prefix abc"),
  ),
  new TransformExample(
    parameters = Array("regex", "\\w*"),
    input1 = Array("(TestValue123)"),
    throwsException = classOf[ValidationException]
  ),
  new TransformExample(
    parameters = Array("regex", "[a-d]*"),
    input1 = Array("abcde"),
    throwsException = classOf[ValidationException]
  ),
  new TransformExample(
    parameters = Array("regex", "Prefix \\w\\w\\w"),
    input1 = Array("Prefixabc"),
    throwsException = classOf[ValidationException]
  ),
))
case class ValidateRegex(
  @Param(value = "regular expression")
  regex: String = "\\w*"
) extends SimpleTransformer {

  private val compiledRegex = new Regex(regex)

  override def evaluate(value: String): String = {
    if(!compiledRegex.pattern.matcher(value).matches()) {
      throw new ValidationException(s"Value '$value' did not match regular expression '$regex'.")
    } else {
      value
    }
  }
}

object ValidateRegex {
  final val pluginId = "validateRegex"
}