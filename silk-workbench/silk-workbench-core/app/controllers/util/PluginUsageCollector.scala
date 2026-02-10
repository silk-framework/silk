package controllers.util

import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.rule.input.{Input, PathInput, TransformInput}
import org.silkframework.rule.similarity.{Aggregation, Comparison, SimilarityOperator}
import org.silkframework.rule.{LinkSpec, TransformRule, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{AnyPlugin, PluginDescription}
import org.silkframework.util.Identifier
import org.silkframework.workspace.Project
import org.silkframework.workspace.activity.workflow.Workflow

object PluginUsageCollector {

  /**
   * Collects all plugin usages in the given task specification.
   * Includes all plugins that are directly used in the task specification and all plugins that are used in any referenced task specifications.
   *
   * @return A map from plugin identifier to plugin description.
   */
  def pluginUsages(project: Project, taskSpec: TaskSpec)
                  (implicit user: UserContext): Map[Identifier, PluginDescription[AnyPlugin]] = {
    taskSpec match {
      case workflow: Workflow =>
        workflow.nodes.flatMap(node => pluginUsages(project, project.anyTask(node.nodeId))).toMap
      case transformSpec: TransformSpec =>
        transformSpec.rules.allRules.flatMap(pluginUsagesInTransform).toMap
      case linkSpec: LinkSpec =>
        linkSpec.rule.operator.toSeq.flatMap(pluginUsagesInSimilarityOperator).toMap
      case customTask: CustomTask =>
        Map(spec(customTask))
      case dataset: DatasetSpec[_] =>
        Map(spec(dataset.plugin))
      case _ =>
        Map.empty
    }
  }

  private def pluginUsagesInTransform(transformRule: TransformRule): Map[Identifier, PluginDescription[AnyPlugin]] =  {
    transformRule.rules.allRules.flatMap(pluginUsagesInTransform).toMap ++ pluginUsagesInInputOperator(transformRule.operator)
  }

  private def pluginUsagesInSimilarityOperator(op: SimilarityOperator): Map[Identifier, PluginDescription[AnyPlugin]] = {
    op match {
      case comparison: Comparison =>
        comparison.inputs.flatMap(pluginUsagesInInputOperator).toMap + spec(comparison.metric)
      case aggregation: Aggregation =>
        aggregation.operators.flatMap(pluginUsagesInSimilarityOperator).toMap + spec(aggregation.aggregator)
    }
  }

  private def pluginUsagesInInputOperator(op: Input): Map[Identifier, PluginDescription[AnyPlugin]] = {
    op match {
      case transform: TransformInput =>
        transform.inputs.flatMap(pluginUsagesInInputOperator).toMap + spec(transform.transformer)
      case _: PathInput =>
        Map.empty
    }
  }

  private def spec(plugin: AnyPlugin): (Identifier, PluginDescription[AnyPlugin]) = {
    plugin.pluginSpec.id -> plugin.pluginSpec
  }

}
