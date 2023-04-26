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

package org.silkframework.rule.plugins.transformer.combine

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

@Plugin(
  id = "concat",
  categories = Array("Combine"),
  label = "Concatenate",
  description = "Concatenates strings from multiple inputs."
)
@TransformExamples(Array(
  new TransformExample(
    output = Array()
  ),
  new TransformExample(
    input1 = Array("a"),
    output = Array("a")
  ),
  new TransformExample(
    input1 = Array("a"),
    input2 = Array("b"),
    output = Array("ab")
  ),
  new TransformExample(
    parameters = Array("glue", "-"),
    input1 = Array("First"),
    input2 = Array("Last"),
    output = Array("First-Last")
  ),
  new TransformExample(
    parameters = Array("glue", "-"),
    input1 = Array("First"),
    input2 = Array("Second", "Third"),
    output = Array("First-Second", "First-Third")
  ),
  new TransformExample(
    parameters = Array("glue", "-"),
    input1 = Array("First"),
    input2 = Array(""),
    input3 = Array("Second"),
    output = Array("First--Second")
  ),
  new TransformExample(
    parameters = Array("glue", "-"),
    input1 = Array("First"),
    input2 = Array(),
    input3 = Array("Second"),
    output = Array()
  ),
  new TransformExample(
    parameters = Array("glue", "-", "missingValuesAsEmptyStrings", "true"),
    input1 = Array("First"),
    input2 = Array(),
    input3 = Array("Second"),
    output = Array("First--Second")
  ),
  new TransformExample(
    parameters = Array("glue", "\\n"),
    input1 = Array("First"),
    input2 = Array("Second"),
    output = Array("First\nSecond")
  ),
  new TransformExample(
    parameters = Array("glue", "\\t\\\\\\a"),
    input1 = Array("First"),
    input2 = Array("Second"),
    output = Array("First\t\\\\aSecond")
  )
))
case class ConcatTransformer(
  @Param("Separator to be inserted between two concatenated strings. The text can contain escaped characters \\n, \\t and" +
    " \\\\ that are replaced by a newline, tab or backslash respectively.")
  glue: String = "",
  @Param("Handle missing values as empty strings.")
  missingValuesAsEmptyStrings: Boolean = false) extends Transformer {

  // glue with escaped char sequences (\\, \n, \t) converted to actual character.
  lazy val parsedGlue: String = ConcatTransformer.parseGlue(glue)

  override def apply(values: Seq[Seq[String]]): Seq[String] = {

    if(values.isEmpty) {
      Seq.empty
    } else if(values.size == 1) {
      values.head
    } else {
      val preprocessed = if (missingValuesAsEmptyStrings) {
        for (value <- values) yield {
          if (value.isEmpty) {
            Seq("")
          }
          else {
            value
          }
        }
      } else {
        values
      }
      for (sequence <- cartesianProduct(preprocessed)) yield evaluate(sequence)
    }
  }

  private def cartesianProduct(strings: Seq[Seq[String]]): Seq[List[String]] = {
    if (strings.tail.isEmpty) {
      for (string <- strings.head) yield string :: Nil
    }
    else {
      for (string <- strings.head; seq <- cartesianProduct(strings.tail)) yield string :: seq
    }
  }

  private def evaluate(strings: Seq[String]) = {
    strings.mkString(parsedGlue)
  }
}

object ConcatTransformer {
  /** Converts escape sequences into their actual character. Supports: "\\", "\n" nad "\t" */
  def parseGlue(glue: String): String = {
    if(glue.contains("\\")) {
      var lastCharEscapingBackSlash = false
      val sb = new StringBuilder()
      glue.foreach(c => {
        if(lastCharEscapingBackSlash) {
          c match {
            case '\\' =>
              sb.append('\\')
            case 'n' =>
              sb.append('\n')
            case 't' =>
              sb.append('\t')
            case other: Char =>
              sb.append('\\').append(other)
          }
          lastCharEscapingBackSlash = false
        } else {
          if(c == '\\') {
            lastCharEscapingBackSlash = true
          } else {
            sb.append(c)
          }
        }
      })
      sb.toString()
    } else {
      glue
    }
  }
}