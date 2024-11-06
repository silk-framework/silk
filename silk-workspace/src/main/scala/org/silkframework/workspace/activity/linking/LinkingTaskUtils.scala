package org.silkframework.workspace.activity.linking

import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{DataSource, Dataset, DatasetSpec, EmptySource, LinkSink}
import org.silkframework.rule.{DatasetSelection, LinkSpec, LinkageRule, TaskContext, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.DPair
import org.silkframework.workspace.ProjectTask
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
      task.project.taskOption[TransformSpec](selection.inputId) match {
        case Some(transformTask) =>
          transformTask.asDataSource(selection.typeUri)
        case None =>
          task.project.taskOption[GenericDatasetSpec](selection.inputId)
            .map(_.data.source)
            // Only datasets and transform inputs supported, everything else will be empty.
            .getOrElse(EmptySource)
      }
    }

    /**
      * Retrieves all link sinks for this linking task.
      */
    def linkSink(implicit userContext: UserContext): Option[LinkSink] = {
      task.data.output.flatMap(o => task.project.taskOption[DatasetSpec[Dataset]](o)).map(_.data.linkSink)
    }

    /**
     * Generates the task context assuming that this task is executed standalone (i.e., not in a workflow)
     */
    def taskContext(implicit userContext: UserContext): TaskContext = {
      implicit val pluginContext: PluginContext = PluginContext.fromProject(task.project)
      val inputTasks = task.dataSelections.map(selection => task.project.anyTask(selection.inputId)(pluginContext.user))
      TaskContext(inputTasks, pluginContext)
    }

    /**
     * Returns the linking rule with the task context.
     */
    def ruleWithContext(implicit userContext: UserContext): LinkageRule = {
      task.data.rule.withContext(taskContext)
    }
  }

}
