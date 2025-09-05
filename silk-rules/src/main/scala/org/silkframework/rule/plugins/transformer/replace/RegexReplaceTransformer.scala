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

package org.silkframework.rule.plugins.transformer.replace

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

import scala.util.matching.Regex

@Plugin(
  id = "regexReplace",
  categories = Array("Replace"),
  label = "Regex replace",
  description = "Replace all occurrences of a regular expression in a string." +
    " If no replacement is given, the occurrences of the regular expression will be deleted.",
  documentationFile = "RegexReplaceTransformer.md"
)
@TransformExamples(Array(
  new TransformExample(
    description = "Removes all digits by replacing them with an empty string",
    parameters = Array("regex", "[^0-9]*"),
    input1 = Array("a0b1c2"),
    output = Array("012")
  ),
  new TransformExample(
    description = "Removes all letters by replacing them with an empty string",
    parameters = Array("regex", "[a-z]*"),
    input1 = Array("abcdef1"),
    output = Array("1")
  ),
  new TransformExample(
    description = "Removes all vowels by replacing them with an empty string",
    parameters = Array("regex", "[aeyiuoAEYIUO]*"),
    input1 = Array("Dwalin", "Balin", "Kili", "Fili", "Dori", "Nori", "Ori", "Oin", "Gloin", "Bifur", "Bofur", "Bombur", "Thorin"),
    output = Array("Dwln", "Bln", "Kl", "Fl", "Dr", "Nr", "r", "n", "Gln", "Bfr", "Bfr", "Bmbr", "Thrn")
  ),
  new TransformExample(
    description = "Removes all consonants by replacing them with an empty string",
    parameters = Array("regex", "[^aeyiuoAEYIUO]*"),
    input1 = Array("Dwalin", "Balin", "Kili", "Fili", "Dori", "Nori", "Ori", "Oin", "Gloin", "Bifur", "Bofur", "Bombur", "Thorin"),
    output = Array("ai", "ai", "ii", "ii", "oi", "oi", "Oi", "Oi", "oi", "iu", "ou", "ou", "oi")
  ),
  new TransformExample(
    description = "Replaces all vowels with a common vowel",
    parameters = Array("regex", "[aeyiuoAEYIUO]{1}", "replace", "a"),
    input1 = Array("Dwalin", "Balin", "Kili", "Fili", "Dori", "Nori", "Ori", "Oin", "Gloin", "Bifur", "Bofur", "Bombur", "Thorin"),
    output = Array("Dwalan", "Balan", "Kala", "Fala", "Dara", "Nara", "ara", "aan", "Glaan", "Bafar", "Bafar", "Bambar", "Tharan")
  ),
  new TransformExample(
    description = "Replaces all vowels with a common double vowel",
    parameters = Array("regex", "[aeyiuoAEYIUO]{1}", "replace", "aa"),
    input1 = Array("Dwalin", "Balin", "Kili", "Fili", "Dori", "Nori", "Ori", "Oin", "Gloin", "Bifur", "Bofur", "Bombur", "Thorin"),
    output = Array("Dwaalaan", "Baalaan", "Kaalaa", "Faalaa", "Daaraa", "Naaraa", "aaraa", "aaaan", "Glaaaan", "Baafaar", "Baafaar", "Baambaar", "Thaaraan")
  ),
))
case class RegexReplaceTransformer(@Param(value = "The regular expression to match", example = "\\s*")
                                   regex: String,
                                   @Param(value = "The replacement of each match")
                                   replace: String = "") extends SimpleTransformer{
  private val compiledRegex = new Regex(regex)

  def evaluate(value: String): String = {
    compiledRegex.replaceAllIn(value, replace)
  }
}
