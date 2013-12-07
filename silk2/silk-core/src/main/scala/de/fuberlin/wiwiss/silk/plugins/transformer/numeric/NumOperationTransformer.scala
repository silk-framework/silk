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

package de.fuberlin.wiwiss.silk.plugins.transformer.numeric

import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.util.StringUtils.DoubleLiteral

/**
 * Applies a numeric operation.
 *
 * @author Julien Plu
 * @author Robert Isele
 */
@Plugin(
  id = "numOperation",
  categories = Array("numeric"),
  label = "Numeric Operation",
  description =
    """ Applies a numeric operation.
      | Accepts two paramters:
      | operator: One of '+', '-', '*', '/'
      | operand: The operand.
    """.stripMargin
)
class NumOperationTransformer(operator: String, operand: Double) extends SimpleTransformer {
  require(Set("+", "-", "*", "/") contains operator, "Operator must be one of '+', '-', '*', '/'")

  override def evaluate(value: String) = {
    value match {
      case DoubleLiteral(d) => operation(d).toString
      case str => str
    }
  }

  private def operation(value: Double): Double = {
    operator match {
      case "+" => value + operand
      case "-" => value - operand
      case "*" => value * operand
      case "/" => value / operand
    }
  }
}
