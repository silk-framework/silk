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

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.InlineTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.StringUtils.DoubleLiteral

/**
 * Applies a numeric operation.
 *
 * @author Julien Plu
 * @author Robert Isele
 */
@Plugin(
  id = NumOperationTransformer.pluginId,
  categories = Array("Numeric"),
  label = "Numeric operation",
  description = """Applies one of the four basic arithmetic operators to the sequence of input values.""",
  documentationFile = "NumOperationTransformer.md",
  relatedPlugins = Array(
    new PluginReference(
      id = AggregateNumbersTransformer.pluginId,
      description = "The Numeric operation plugin reduces all input numbers into one result using one arithmetic operator and fails when any value is not a number. The Aggregate numbers plugin also reduces to one result, but it ignores non-numeric values and shifts the operator set toward aggregation semantics such as minimum, maximum, and average."
    ),
    new PluginReference(
      id = PhysicalQuantityExtractor.pluginId,
      description = "The Extract physical quantity plugin converts number-and-unit text into base-unit numeric values as plain numeric output. The Numeric operation plugin combines those numeric values using one arithmetic operator across the operand sequence."
    ),
    new PluginReference(
      id = NumReduceTransformer.pluginId,
      description = "Numeric reduce strips non-numeric characters from each value. Numeric operation then applies an arithmetic operator across the resulting values, since it throws on any input that is not a number."
    )
  )
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array("operator", "+"),
    input1 = Array("1"),
    input2 = Array("1"),
    output = Array("2.0")
  ),
  new TransformExample(
    parameters = Array("operator", "-"),
    input1 = Array("1"),
    input2 = Array("1"),
    output = Array("0.0")
  ),
  new TransformExample(
    parameters = Array("operator", "*"),
    input1 = Array("5"),
    input2 = Array("6"),
    output = Array("30.0")
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
    output = Array("1.0")
  ),
  new TransformExample(
    parameters = Array("operator", "+"),
    input1 = Array("1", "1"),
    input2 = Array("1"),
    output = Array("3.0")
  ),
  new TransformExample(
    parameters = Array("operator", "/"),
    input1 = Array("1"),
    input2 = Array("0"),
    output = Array("Infinity")
  ),
))
case class NumOperationTransformer(
  @Param("The operator to be applied to all values. One of `+`, `-`, `*`, `/`")
  operator: String
) extends InlineTransformer {

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

object NumOperationTransformer {
  final val pluginId = "numOperation"
}