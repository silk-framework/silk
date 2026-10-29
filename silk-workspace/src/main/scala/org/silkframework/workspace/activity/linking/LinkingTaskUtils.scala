package org.silkframework.workspace.activity.linking

import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{DataSource, Dataset, DatasetSpec, EmptySource, LinkSink}
import org.silkframework.execution.ExecutorRegistry
import org.silkframework.rule.{DatasetSelection, LinkSpec, LinkageRuleExecution, TaskContext, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.DPair
import org.silkframework.workspace.{ProjectTask}
import org.silkframework.workspace.activity.transform.TransformTaskUtils._

/**
  * Adds additional methods to linking tasks.
  */
object LinkingTaskUtils {

  implicit class LinkingTask(task: ProjectTask[LinkSpec]) {

    /**
      * Retrieves both data sources for this linking task.
      */
    def dataSources(implicit userContext: UserContext): DPair[DataSource] = {
      task.data.dataSelections.map(dataSource)
    }

    /**
      * Retrieves a specific data source for this linking task.
      */
    def dataSource(selection: DatasetSelection)
                  (implicit userContext: UserContext): DataSource = {
      task.project.taskOption[TransformSpec](selection.inputTaskId) match {
        case Some(transformTask) =>
          transformTask.asDataSource(selection.typeUri)
        case None =>
          task.project.taskOption[GenericDatasetSpec](selection.inputTaskId)
            .map(ExecutorRegistry.access(_).source)
            // Only datasets and transform inputs supported, everything else will be empty.
            .getOrElse(EmptySource)
      }
    }

    /**
      * Retrieves all link sinks for this linking task.
      */
    def linkSink(implicit userContext: UserContext): Option[LinkSink] = {
      task.data.output.flatMap(o => task.project.taskOption[DatasetSpec[Dataset]](o)).map(ExecutorRegistry.access(_).linkSink)
    }

    /**
     * Generates the task context assuming that this task is executed standalone (i.e., not in a workflow)
     */
    def taskContext(implicit userContext: UserContext): TaskContext = {
      implicit val pluginContext: PluginContext = PluginContext.fromTask(task, task.project)
      val inputTasks = task.dataSelections.toSeq.flatMap(selection => selection.inputTaskId.map(id => task.project.anyTask(id)(pluginContext.user)))
      TaskContext(inputTasks, pluginContext)
    }

    /**
     * Returns the linking rule executor resolved against the task context.
     */
    def ruleExecution(implicit userContext: UserContext): LinkageRuleExecution = {
      task.data.rule.execution(taskContext)
    }
  }

}
