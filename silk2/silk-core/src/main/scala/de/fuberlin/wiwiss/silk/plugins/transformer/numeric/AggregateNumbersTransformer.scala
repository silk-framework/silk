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
 * Aggregates all numbers in this set using a mathematical operation..
 *
 * @author Robert Isele
 */
@Plugin(
  id = "aggregateNumbers",
  categories = Array("Numeric"),
  label = "Aggregate Numbers",
  description =
    """ | Aggregates all numbers in this set using a mathematical operation.
      | Accepts one paramter:
      |   operator: One of '+', '*'"""
)
class AggregateNumbersTransformer(operator: String) extends Transformer {
  require(Set("+", "*") contains operator, "Operator must be one of '+', '*'")

  def apply(values: Seq[Set[String]]): Set[String] = {
    // Collect all numbers
    val numbers = values.flatten.collect { case DoubleLiteral(d) => d }
    // Aggregate numbers
    val result = operator match {
      case "+" => numbers.fold(0.0)(_ + _)
      case "*" => numbers.fold(1.0)(_ * _)
    }
    // Return result
    Set(result.toString)
  }
}
