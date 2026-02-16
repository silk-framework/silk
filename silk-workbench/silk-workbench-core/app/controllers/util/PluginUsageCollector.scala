package controllers.util

import controllers.workspaceApi.coreApi.PluginUsage
import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.rule.input.{Input, PathInput, TransformInput}
import org.silkframework.rule.similarity.{Aggregation, Comparison, SimilarityOperator}
import org.silkframework.rule.{LinkSpec, TransformRule, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.AnyPlugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.workflow.Workflow

object PluginUsageCollector {

  /**
   * Collects all plugin usages in the given task specification.
   * Includes all plugins that are directly used in the task specification and all plugins that are used in any referenced task specifications.
   */
  def pluginUsages(task: ProjectTask[_ <: TaskSpec])
                  (implicit user: UserContext): Seq[PluginUsage] = {
    task.data match {
      case workflow: Workflow =>
        workflow.nodes.flatMap(node => pluginUsages(task.project.anyTask(node.task)))
      case transformSpec: TransformSpec =>
        val collector = new RuleUsageCollector(task)
        transformSpec.rules.allRules.flatMap(collector.pluginUsagesInTransform)
      case linkSpec: LinkSpec =>
        val collector = new RuleUsageCollector(task)
        linkSpec.rule.operator.toSeq.flatMap(collector.pluginUsagesInSimilarityOperator)
      case customTask: CustomTask =>
        Seq(PluginUsage.forTask(task, customTask.pluginSpec))
      case dataset: DatasetSpec[_] =>
        Seq(PluginUsage.forTask(task, dataset.plugin.pluginSpec))
      case _ =>
        Seq.empty
    }
  }

  private class RuleUsageCollector(task: ProjectTask[_ <: TaskSpec]) {

    def pluginUsagesInTransform(transformRule: TransformRule): Seq[PluginUsage] =  {
      transformRule.rules.allRules.flatMap(pluginUsagesInTransform) ++ pluginUsagesInInputOperator(transformRule.operator)
    }

    def pluginUsagesInSimilarityOperator(op: SimilarityOperator): Seq[PluginUsage] = {
      op match {
        case comparison: Comparison =>
          comparison.inputs.flatMap(pluginUsagesInInputOperator) :+ usage(comparison.metric)
        case aggregation: Aggregation =>
          aggregation.operators.flatMap(pluginUsagesInSimilarityOperator) :+ usage(aggregation.aggregator)
      }
    }

    private def pluginUsagesInInputOperator(op: Input): Seq[PluginUsage] = {
      op match {
        case transform: TransformInput =>
          transform.inputs.flatMap(pluginUsagesInInputOperator) :+ usage(transform.transformer)
        case _: PathInput =>
          Seq.empty
      }
    }

    private def usage(plugin: AnyPlugin): PluginUsage = {
      PluginUsage.forTask(task, plugin.pluginSpec)
    }
  }

}
