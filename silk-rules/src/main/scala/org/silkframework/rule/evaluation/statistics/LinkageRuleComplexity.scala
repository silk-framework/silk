/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.rule.evaluation.statistics


import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.rule.similarity.{Aggregation, Comparison}
import org.silkframework.rule.{LinkageRule, Operator}

/**
 * Complexity measures of a linkage rule.
 *
 * @param size The number of operators in total
 * @param comparisonCount The number of comparisons.
 * @param transformationCount The number of transformations.
 */
case class LinkageRuleComplexity(size: Int, comparisonCount: Int, transformationCount: Int)

/**
 * Evaluates the complexity of a linkage rule.
 */
object LinkageRuleComplexity {
  /**
   * Evaluates the complexity of a linkage rule.
   */
  def apply(linkageRule: LinkageRule): LinkageRuleComplexity = {
    val ops = collectOperators(linkageRule)

    LinkageRuleComplexity(
      size = ops.size,
      comparisonCount = ops.count(_.isInstanceOf[Comparison]),
      transformationCount = ops.count(_.isInstanceOf[TransformInput])
    )
  }

  /**
   * Collects all operators of the linkage rule.
   */
  private def collectOperators(linkageRule: LinkageRule): Iterable[Operator] = {
    linkageRule.operator.toSeq.flatMap(collectOperators)
  }

  /**
   * Collects all sub operators.
   */
  private def collectOperators(root: Operator): Iterable[Operator] = root match {
    case Aggregation(_, _, _, ops) => root +: ops.flatMap(collectOperators)
    case Comparison(_, _, _, _, _, inputs) => root +: inputs.flatMap(collectOperators)
    case TransformInput(_, _, inputs) => root +: inputs.flatMap(collectOperators)
    case PathInput(_, _) => Iterable(root)
  }
}
