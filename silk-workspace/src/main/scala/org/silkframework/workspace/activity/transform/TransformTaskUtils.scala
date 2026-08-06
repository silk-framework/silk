package org.silkframework.workspace.activity.transform

import org.silkframework.config.CustomTask
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{DataSource, EntitySink}
import org.silkframework.execution.{ExecutorRegistry, TaskException}
import org.silkframework.rule.{TaskContext, TransformSpec, TransformedDataSource}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.validation.{BadUserInputException, ValidationException}
import org.silkframework.util.Uri
import org.silkframework.workspace.{ProjectTask}

/**
  * Adds additional methods to transform tasks.
  */
object TransformTaskUtils {

  implicit class TransformTaskExtension(task: ProjectTask[TransformSpec]) {

    /**
      * Retrieves the input dataset task of this transform task.
      * Note that the input of a transform task is not necessarily a dataset, but may also be another transform or a custom task.
      *
      * @throws BadUserInputException If the input task exists, but is not a dataset.
      * @throws org.silkframework.workspace.exceptions.TaskNotFoundException If no task with the configured input identifier exists.
      */
    def inputDatasetTask(implicit userContext: UserContext): ProjectTask[GenericDatasetSpec] = {
      val inputId = task.data.selection.requiredInputId()
      task.project.taskOption[GenericDatasetSpec](inputId).getOrElse {
        val inputTask = task.project.anyTask(inputId) // Throws a 'task not found' error if the input task does not exist at all
        throw BadUserInputException(s"The input task ${inputTask.labelAndId} of transform task ${task.labelAndId} is not a dataset. Only dataset inputs are supported for this feature.")
      }
    }

    /**
      * Retrieves the data source for this transform task.
      */
    def dataSource(implicit userContext: UserContext): DataSource = {
      val sourceId = task.data.selection.requiredInputId()
      task.project.taskOption[CustomTask](sourceId) match {
        case Some(customTask) =>
          throw TaskException(s"Task ${customTask.id} of type 'Other' is not supported as data source. Evaluate and Execute actions are thus not working.")
        case None =>
          task.project.taskOption[TransformSpec](sourceId) match {
            case Some(transformTask) =>
              transformTask.asDataSource(transformTask.data.selection.typeUri)
            case None =>
              ExecutorRegistry.access(inputDatasetTask).source
          }
      }
    }

    /**
      * Converts this transform task to a data source.
      */
    def asDataSource(typeUri: Uri)
                    (implicit userContext: UserContext): DataSource = {
      val transformSpec = task.data
      val source = ExecutorRegistry.access(task.inputDatasetTask).source

      // Find the rule that generates the selected type
      if(typeUri.uri.isEmpty) {
        new TransformedDataSource(source, transformSpec.inputSchema, transformSpec.mappingRule, task)
      } else {
        transformSpec.ruleSchemataWithoutEmptyObjectRules.find(_.transformRule.rules.typeRules.map(_.typeUri).contains(typeUri)) match {
          case Some(ruleSchemata) =>
            new TransformedDataSource(source, ruleSchemata.inputSchema, ruleSchemata.transformRule, task)
          case None =>
            throw new ValidationException(s"No rule matching target type $typeUri found.")
        }
      }
    }

    /**
      * Retrieves all entity sinks for this transform task.
      */
    def entitySink(implicit userContext: UserContext): Option[EntitySink] = {
      task.data.output.flatMap(o => task.project.taskOption[GenericDatasetSpec](o)).map(ExecutorRegistry.access(_).entitySink)
    }

    /**
      * Retrieves all error entity sinks for this transform task.
      */
    def errorEntitySink(implicit userContext: UserContext): Option[EntitySink] = {
      task.data.errorOutput.flatMap(o => task.project.taskOption[GenericDatasetSpec](o)).map(ExecutorRegistry.access(_).entitySink)
    }

    /**
     * Generates the task context assuming that this task is executed standalone (i.e., not in a workflow)
     */
    def taskContext(implicit pluginContext: PluginContext): TaskContext = {
      val inputTasks = task.selection.inputTaskId.toSeq.map(id => task.project.anyTask(id)(pluginContext.user))
      TaskContext(inputTasks, pluginContext)
    }
  }

}
