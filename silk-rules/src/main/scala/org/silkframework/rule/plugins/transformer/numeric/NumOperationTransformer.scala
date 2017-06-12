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

package org.silkframework.rule.plugins.transformer.numeric

import org.silkframework.rule.input.{TransformExample, TransformExamples, Transformer}
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.StringUtils.DoubleLiteral

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
        | The operator is applied to all input values.
        | Accepts one parameter:
        |   operator: One of '+', '-', '*', '/'"""
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array("operator", "+"),
    input1 = Array("1"),
    input2 = Array("1"),
    output = Array("2")
  ),
  new TransformExample(
    parameters = Array("operator", "-"),
    input1 = Array("1"),
    input2 = Array("1"),
    output = Array("0")
  ),
  new TransformExample(
    parameters = Array("operator", "*"),
    input1 = Array("5"),
    input2 = Array("6"),
    output = Array("30")
  ),
  new TransformExample(
    parameters = Array("operator", "/"),
    input1 = Array("5"),
    input2 = Array("2"),
    output = Array("2.5")
  ),
  new TransformExample(
    parameters = Array("operator", "+"),
    input1 = Array("1"),
    input2 = Array("no number"),
    output = Array()
  ),
  new TransformExample(
    parameters = Array("operator", "*"),
    input1 = Array("1"),
    input2 = Array(),
    output = Array("1")
  ),
  new TransformExample(
    parameters = Array("operator", "+"),
    input1 = Array("1", "1"),
    input2 = Array("1"),
    output = Array("3")
  )
))
case class NumOperationTransformer(operator: String) extends Transformer {

  require(Set("+", "-", "*", "/") contains operator, "Operator must be one of '+', '-', '*', '/'")

  def apply(values: Seq[Seq[String]]): Seq[String] = {
    val operands = values.flatMap(_.map(parse))
    Seq(operands.reduce(operation).toString)
  }

  def parse(value: String): Double = {
    value match {
      case DoubleLiteral(d) => d
      case _ => throw new ValidationException(s"Input value $value must be a number.")
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
