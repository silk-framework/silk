package controllers.util

import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.rule.input.{Input, PathInput, TransformInput}
import org.silkframework.rule.similarity.{Aggregation, Comparison, SimilarityOperator}
import org.silkframework.rule.{LinkSpec, TransformRule, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier

object PluginUsageCollector {

  def pluginUsages(taskSpec: TaskSpec)(implicit userContext: UserContext): Set[Identifier] = {
    taskSpec match {
      case transformSpec: TransformSpec =>
        transformSpec.rules.allRules.flatMap(pluginUsagesInTransform).toSet
      case linkSpec: LinkSpec =>
        linkSpec.rule.operator.toSeq.flatMap(pluginUsagesInSimilarityOperator).toSet
      case customTask: CustomTask =>
        Set(customTask.pluginSpec.id)
      case _ =>
        Set.empty
    }
  }

  private def pluginUsagesInTransform(transformRule: TransformRule): Set[Identifier] =  {
    transformRule.rules.allRules.flatMap(pluginUsagesInTransform).toSet ++ pluginUsagesInInputOperator(transformRule.operator)
  }

  private def pluginUsagesInSimilarityOperator(op: SimilarityOperator): Set[Identifier] = {
    op match {
      case comparison: Comparison =>
        Set(comparison.metric.pluginSpec.id)
      case aggregation: Aggregation =>
        aggregation.operators.flatMap(pluginUsagesInSimilarityOperator).toSet + aggregation.aggregator.pluginSpec.id
    }
  }

  private def pluginUsagesInInputOperator(op: Input): Set[Identifier] = {
    op match {
      case transform: TransformInput =>
        transform.inputs.flatMap(pluginUsagesInInputOperator).toSet + transform.transformer.pluginSpec.id
      case _: PathInput =>
        Set.empty
    }
  }

}
