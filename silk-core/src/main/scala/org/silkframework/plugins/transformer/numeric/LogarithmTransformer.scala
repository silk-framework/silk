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

package org.silkframework.plugins.transformer.numeric

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.StringUtils._

@Plugin(
  id = "logarithm",
  categories = Array("Numeric"),
  label = "Logarithm",
  description = "Transforms all numbers by applying the logarithm function. Non-numeric values are left unchanged."
)
case class LogarithmTransformer(base: Int = 10) extends SimpleTransformer {
  override def evaluate(value: String) = {
    value match {
      case DoubleLiteral(d) => (math.log(d) / math.log(base)).toString
      case str => str
    }
  }
}