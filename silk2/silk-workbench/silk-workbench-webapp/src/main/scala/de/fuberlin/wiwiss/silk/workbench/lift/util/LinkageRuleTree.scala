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