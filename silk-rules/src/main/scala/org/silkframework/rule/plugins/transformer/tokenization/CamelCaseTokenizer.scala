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

package org.silkframework.rule.plugins.transformer.tokenization

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.Plugin

import scala.collection.mutable.ArrayBuffer

@Plugin(
  id = "camelcasetokenizer",
  categories = Array("Tokenization"),
  label = "Camel case tokenizer",
  description = "Tokenizes a camel case string. That is it splits strings between a lower case characted and an upper case character."
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("camelCaseString"),
    output = Array("camel", "Case", "String")
  ),
  new TransformExample(
    input1 = Array("nocamelcase"),
    output = Array("nocamelcase")
  )
))
case class CamelCaseTokenizer() extends Transformer {
  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    values.reduce(_ ++ _).flatMap(splitOnCamelCase)
  }
  
  def splitOnCamelCase(value: String): Seq[String] = {
    val tokens = ArrayBuffer[String]()
    var lastWasLowerCase = false
    var sb = new StringBuilder
    for(c <- value) {
      if(c.isUpper && lastWasLowerCase) {
        tokens += sb.toString
        sb = new StringBuilder
      }
      sb.append(c)
      lastWasLowerCase = c.isLower
    }
    if(sb.nonEmpty) {
      tokens += sb.toString
    }
    tokens
  }
}
