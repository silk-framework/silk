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

import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.util.StringUtils.DoubleLiteral

/**
 * Applies a numeric operation.
 *
 * @author Julien Plu
 * @author Robert Isele
 */
@Plugin(
  id = "numOperation",
  categories = Array("Numeric"),
  label = "Numeric Operation",
  description =
    """ | Applies a numeric operation to the values of multiple input operators.
        | Each input operator is expected to provide one numeric value.
        | If an input operator provides multiple values, all of its values are summed up before the operation.
        | Accepts two paramters:
        |   operator: One of '+', '-', '*', '/'
        |   operand: Final operand that is added/subtracted/etc. after all inputs. """
)
case class NumOperationTransformer(operator: String, operand: Double) extends Transformer {

  require(Set("+", "-", "*", "/") contains operator, "Operator must be one of '+', '-', '*', '/'")

  def apply(values: Seq[Set[String]]): Set[String] = {
    val operands = values.map(_.map(parse).sum) :+ operand
    Set(operands.reduce(operation).toString)
  }

  def parse(value: String): Double = {
    value match {
      case DoubleLiteral(d) => d
      case str => 0.0
    }
  }

  private def operation(value1: Double, value2: Double): Double = {
    operator match {
      case "+" => value1 + value2
      case "-" => value1 - value2
      case "*" => value1 * value2
      case "/" => value1 / value2
    }
  }
}
