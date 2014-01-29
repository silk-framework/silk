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

package de.fuberlin.wiwiss.silk.plugins.transformer.tokenization

import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import scala.collection.mutable.HashSet

@Plugin(
  id = "camelcasetokenizer",
  categories = Array("Tokenization"),
  label = "Camel Case Tokenizer",
  description = "Tokenizes a camel case string. That is it splits strings between a lower case characted and an upper case character."
)
class CamelCaseTokenizer extends Transformer {
  override def apply(values: Seq[Set[String]]): Set[String] = {
    values.reduce(_ ++ _).flatMap(splitOnCamelCase)
  }
  
  def splitOnCamelCase(value: String): Set[String] = {
    val tokens = new HashSet[String]
    var lastWasLowerCase = false
    var sb = new StringBuffer
    for(c <- value) {
      if(c.isUpper && lastWasLowerCase) {
        tokens += sb.toString
        sb = new StringBuffer
      }
      sb.append(c)
      lastWasLowerCase = c.isLower
    }
    if(sb.length>0)
      tokens += sb.toString
    tokens.toSet
  }
}
