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

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.util.StringUtils.DoubleLiteral

/**
 * Aggregates all numbers in this set using a mathematical operation..
 *
 * @author Robert Isele
 */
@Plugin(
  id = "aggregateNumbers",
  categories = Array("Numeric"),
  label = "Aggregate Numbers",
  description = "Aggregates all numbers in this set using a mathematical operation."
)
case class AggregateNumbersTransformer(
  @Param("One of '+', '*', 'min', 'max', 'average'.")
  operator: String) extends Transformer {

  require(Set("+", "*", "min", "max", "average") contains operator, "Operator must be one of '+', '*', 'min', 'max', 'average'")

  def apply(values: Seq[Seq[String]]): Seq[String] = {
    // Collect all numbers
    val numbers = values.flatten.collect { case DoubleLiteral(d) => d }
    // Aggregate numbers
    val result = operator match {
      case _ if numbers.isEmpty => 0.0
      case "+" => numbers.sum
      case "*" => numbers.fold(1.0)(_ * _)
      case "min" => numbers.min
      case "max" => numbers.max
      case "average" => numbers.sum / numbers.size
    }
    // Return result
    Seq(result.toString)
  }
}
