package org.silkframework.rule

import org.silkframework.rule.input.{Input, RuleBlockInput, TransformInput}
import org.silkframework.rule.similarity.{Aggregation, Comparison, SimilarityOperator}
import org.silkframework.util.Identifier
import org.silkframework.workspace.annotation.UiAnnotations

import scala.collection.mutable

/** Snapshot of a reusable rule block model used for read-only inspection of evaluated rule block usages. */
case class RuleBlockInspectionSnapshot(ports: IndexedSeq[RuleBlockPort],
                                       operatorTree: Option[Input],
                                       layout: RuleLayout,
                                       uiAnnotations: UiAnnotations)

/** Collection of reusable rule block snapshots keyed by rule block task ID. */
case class RuleBlockInspection(snapshots: Map[Identifier, RuleBlockInspectionSnapshot])

object RuleBlockInspection {
  val empty: RuleBlockInspection = RuleBlockInspection(Map.empty)
}

/** Collects reusable rule block snapshots referenced by transform or linking rules. */
object RuleBlockInspectionCollector {

  def fromTransformRule(rule: TransformRule, taskContext: TaskContext): RuleBlockInspection = {
    val snapshots = mutable.LinkedHashMap[Identifier, RuleBlockInspectionSnapshot]()

    def collectRule(currentRule: TransformRule): Unit = {
      collectInput(currentRule.operator, taskContext, snapshots)
      currentRule.rules.allRules.foreach(collectRule)
    }

    collectRule(rule)
    RuleBlockInspection(snapshots.toMap)
  }

  def fromLinkageRule(rule: LinkageRule, taskContext: TaskContext): RuleBlockInspection = {
    val snapshots = mutable.LinkedHashMap[Identifier, RuleBlockInspectionSnapshot]()
    rule.operator.foreach(operator => collectSimilarityOperator(operator, taskContext, snapshots))
    RuleBlockInspection(snapshots.toMap)
  }

  private def collectSimilarityOperator(operator: SimilarityOperator,
                                        taskContext: TaskContext,
                                        snapshots: mutable.Map[Identifier, RuleBlockInspectionSnapshot]): Unit = {
    operator match {
      case aggregation: Aggregation =>
        aggregation.operators.foreach(child => collectSimilarityOperator(child, taskContext, snapshots))
      case comparison: Comparison =>
        comparison.inputs.foreach(input => collectInput(input, taskContext, snapshots))
    }
  }

  private def collectInput(input: Input,
                           taskContext: TaskContext,
                           snapshots: mutable.Map[Identifier, RuleBlockInspectionSnapshot]): Unit = {
    input match {
      case transformInput: TransformInput =>
        transformInput.inputs.foreach(child => collectInput(child, taskContext, snapshots))
      case RuleBlockInput(_, ruleBlockId, bindings) =>
        if(!snapshots.contains(ruleBlockId)) {
          snapshots.put(ruleBlockId, snapshot(ruleBlockId, taskContext))
        }
        bindings.foreach(binding => collectInput(binding.input, taskContext, snapshots))
      case _ =>
    }
  }

  private def snapshot(ruleBlockId: Identifier, taskContext: TaskContext): RuleBlockInspectionSnapshot = {
    val ruleBlockTask = taskContext.pluginContext.taskResolver.resolveTyped[RuleBlockSpec](ruleBlockId)
    RuleBlockInspectionSnapshot(
      ports = ruleBlockTask.data.ports,
      operatorTree = ruleBlockTask.data.operator,
      layout = ruleBlockTask.data.layout,
      uiAnnotations = ruleBlockTask.data.uiAnnotations
    )
  }
}
