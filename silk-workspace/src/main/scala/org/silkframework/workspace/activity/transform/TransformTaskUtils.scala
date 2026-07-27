package org.silkframework.workspace.activity.transform

import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{DataSource, EntitySink}
import org.silkframework.execution.{ExecutorRegistry, TaskException}
import org.silkframework.rule.{TaskContext, TransformSpec, TransformedDataSource}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException}
import org.silkframework.util.Uri
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.exceptions.TaskNotFoundException

/**
  * Adds additional methods to transform tasks.
  */
object TransformTaskUtils {

  implicit class TransformTaskExtension(task: ProjectTask[TransformSpec]) {

    /**
      * Retrieves the input dataset task of this transform task.
      * Note that the input of a transform task is not necessarily a dataset, but may also be another transform or a custom task.
      *
      * @throws NotFoundException If the input task exists, but is not a dataset.
      * @throws org.silkframework.workspace.exceptions.TaskNotFoundException If no task with the configured input identifier exists.
      */
    def inputDatasetTask(implicit userContext: UserContext): ProjectTask[GenericDatasetSpec] = {
      val inputId = task.data.selection.inputId
      task.project.taskOption[GenericDatasetSpec](inputId).getOrElse {
        val inputTask = task.project.anyTask(inputId) // Throws a 'task not found' error if the input task does not exist at all
        throw new NotFoundException(s"The input task '${inputTask.id}' of transform task '${task.id}' is not a dataset.")
      }
    }

    /**
      * Retrieves the input dataset task of this transform task, if the input is a dataset.
      */
    def inputDatasetTaskOption(implicit userContext: UserContext): Option[ProjectTask[GenericDatasetSpec]] = {
      task.project.taskOption[GenericDatasetSpec](task.data.selection.inputId)
    }

    /**
      * Resolves the input of this transform task, which may be a dataset, another transform task or any task with a fixed output schema.
      */
    def resolveInput(implicit userContext: UserContext): ResolvedTransformInput = {
      ResolvedTransformInput.resolve(task)
    }

    /**
      * Resolves the input of this transform task, if it provides a schema.
      */
    def resolveInputOption(implicit userContext: UserContext): Option[ResolvedTransformInput] = {
      try {
        Some(resolveInput)
      } catch {
        // The input task is missing or provides no schema at all
        case _: BadUserInputException | _: TaskNotFoundException =>
          None
      }
    }

    /**
      * Retrieves the data source for this transform task.
      *
      * @throws org.silkframework.execution.TaskException If the input cannot provide data without executing a workflow.
      */
    def dataSource(implicit userContext: UserContext): DataSource = {
      val resolvedInput =
        try {
          resolveInput
        } catch {
          // The input provides no schema at all, e.g., a custom task without a fixed output schema
          case ex: BadUserInputException => throw TaskException(s"${ex.getMessage} $evaluateAndExecuteNotWorking")
        }
      resolvedInput.dataSourceOption.getOrElse {
        throw TaskException(s"The input task '${task.data.selection.inputId}' of transform task '${task.id}' cannot provide data on its own. " +
          evaluateAndExecuteNotWorking)
      }
    }

    private def evaluateAndExecuteNotWorking = "Evaluate and Execute actions are thus not working."

    /**
      * Converts this transform task to a data source.
      */
    def asDataSource(typeUri: Uri)
                    (implicit userContext: UserContext): DataSource = {
      asDataSource(inputDatasetTask, typeUri)
    }

    /**
      * Converts this transform task to a data source that transforms the entities of the given input dataset.
      * Allows callers that already resolved the input dataset to avoid resolving it a second time.
      */
    def asDataSource(inputDataset: ProjectTask[GenericDatasetSpec], typeUri: Uri)
                    (implicit userContext: UserContext): DataSource = {
      val transformSpec = task.data
      val source = ExecutorRegistry.access(inputDataset).source

      // Find the rule that generates the selected type
      if(typeUri.uri.isEmpty) {
        new TransformedDataSource(source, transformSpec.inputSchema, transformSpec.mappingRule, task)
      } else {
        val ruleSchemata = transformSpec.ruleSchemataForTargetType(typeUri)
        new TransformedDataSource(source, ruleSchemata.inputSchema, ruleSchemata.transformRule, task)
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
      val inputTask = task.project.anyTask(task.selection.inputId)(pluginContext.user)
      TaskContext(Seq(inputTask), pluginContext)
    }
  }

}
