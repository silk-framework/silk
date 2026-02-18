package org.silkframework.workspace

import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.rule.input.{Input, PathInput, TransformInput}
import org.silkframework.rule.similarity.{Aggregation, Comparison, SimilarityOperator}
import org.silkframework.rule.{LinkSpec, TransformRule, TransformSpec}
import org.silkframework.runtime.plugin.{AnyPlugin, PluginDescription}

case class PluginUsage(project: Option[String],
                       projectLabel: Option[String],
                       task: Option[String],
                       taskLabel: Option[String],
                       itemType: Option[String] = None,
                       pluginId: String,
                       pluginLabel: String,
                       link: Option[String],
                       deprecationMessage: Option[String])

object PluginUsage {

  def forTask(task: ProjectTask[_ <: TaskSpec], pluginDesc: PluginDescription[AnyPlugin]): PluginUsage = {
    PluginUsage(
      project = Some(task.project.id.toString),
      projectLabel = Some(task.project.fullLabel),
      task = Some(task.id.toString),
      taskLabel = Some(task.fullLabel),
      itemType = Some(WorkbenchLinks.taskType(task)),
      pluginId = pluginDesc.id.toString,
      pluginLabel = pluginDesc.label,
      link = Some(WorkbenchLinks.editorLink(task)),
      deprecationMessage = pluginDesc.deprecation
    )
  }

  /**
   * Collects all plugin usages in the given task specification.
   * Only includes plugins directly used in the task spec itself (no referenced tasks).
   */
  def pluginUsages(task: ProjectTask[_ <: TaskSpec]): Seq[PluginUsage] = {
    task.data match {
      case transformSpec: TransformSpec =>
        val collector = new RuleUsageCollector(task)
        transformSpec.rules.allRules.flatMap(collector.pluginUsagesInTransform)
      case linkSpec: LinkSpec =>
        val collector = new RuleUsageCollector(task)
        linkSpec.rule.operator.toSeq.flatMap(collector.pluginUsagesInSimilarityOperator)
      case customTask: CustomTask =>
        Seq(forTask(task, customTask.pluginSpec))
      case dataset: DatasetSpec[_] =>
        Seq(forTask(task, dataset.plugin.pluginSpec))
      case _ =>
        Seq.empty
    }
  }

  private class RuleUsageCollector(task: ProjectTask[_ <: TaskSpec]) {

    def pluginUsagesInTransform(transformRule: TransformRule): Seq[PluginUsage] = {
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
      forTask(task, plugin.pluginSpec)
    }
  }

}
