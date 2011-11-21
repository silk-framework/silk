/* 
 * Copyright 2011 Freie Universität Berlin, MediaEvent Services GmbH & Co. KG
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

package de.fuberlin.wiwiss.silk.workbench.lift.util

import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.linkagerule.{Operator, LinkageRule}
import xml.Elem
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.linkagerule.input.{PathInput, TransformInput}

/**
 * Renders a linkage rule as a tree.
 * jQuery.treeview.js and linkageRuleTree.css must be in the header.
 */
object LinkageRuleTree {

  def render(rule: LinkageRule) = {
    <ul class="details-tree">
    { for(aggregation <- rule.operator.toList) yield renderOperator(aggregation) }
    </ul>
  }

  /**
   * Renders a linkage rule operator.
   */
  private def renderOperator(op: Operator): Elem = op match  {
    case Aggregation(id, required, weight, aggregator, operators) => {
      <li>
        <span class="aggregation">Aggregation: {aggregator.pluginId}</span>
        <ul>
        { operators.map(renderOperator) }
        </ul>
      </li>
    }
    case Comparison(id, required, weight, threshold, metric, DPair(input1, input2)) => {
      <li>
        <span class="comparison">Comparison: {metric.pluginId} ({threshold.toString})</span>
        <ul>
          { renderOperator(input1) }
          { renderOperator(input2) }
        </ul>
      </li>
    }
    case TransformInput(id, transformer, inputs) => {
      <li>
        <span class="transformation">Transformation: {transformer.pluginId}</span>
        <ul>
          { inputs.map(renderOperator) }
        </ul>
      </li>
    }
    case PathInput(id, path) => {
      <li>
        <span class="input">Input: {path.serialize}</span>
      </li>
    }
  }
}