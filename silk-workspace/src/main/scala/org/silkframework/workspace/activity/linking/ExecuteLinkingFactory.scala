package org.silkframework.workspace.activity.linking

import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.{AbortExecutionException, ExecutionException, ExecutionType, ExecutorRegistry}
import org.silkframework.rule.{DatasetSelection, LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory

@Plugin(
  id = "ExecuteLinking",
  label = "Execute Linking",
  categories = Array("LinkSpecification"),
  description = "Executes the linking task using the configured execution."
)
case class ExecuteLinkingFactory() extends TaskActivityFactory[LinkSpec, ExecuteLinking] {
  /**
    * Generates a new activity for a given task.
    */
  override def apply(task: ProjectTask[LinkSpec]): Activity[Unit] = {
    new ExecuteLinking(task)
  }
}

class ExecuteLinking(task: ProjectTask[LinkSpec]) extends Activity[Unit] {

  /**
    * Executes this activity.
    *
    * @param context Holds the context in which the activity is executed.
    */
  override def run(context: ActivityContext[Unit])
                  (implicit userContext: UserContext): Unit = {
    implicit val execution: ExecutionType = ExecutorRegistry.execution()

    // Execute inputs
    context.status.update("Loading inputs", 0.1)
    val inputs =
      for((selection, schema) <- task.data.dataSelections zip task.data.entityDescriptions) yield {
        loadInput(selection, schema)
      }

    // Generate links
    context.status.update("Generating links", 0.4)
    val links = ExecutorRegistry.execute(task, inputs, None, execution) match {
      case Some(result) => result
      case None => throw AbortExecutionException("Linking task did not generate any links")
    }

    // Write links to outputs
    context.status.update("Writing links to output", 0.8)
    for(output <- task.data.outputs) {
      val outputTask = task.project.task[GenericDatasetSpec](output)
      ExecutorRegistry.execute(outputTask, Seq(links), None, execution)
    }
  }

  private def loadInput(selection: DatasetSelection, entitySchema: EntitySchema)
                       (implicit execution: ExecutionType,
                        userContext: UserContext): ExecutionType#DataType = {
    val result =
      task.project.taskOption[TransformSpec](selection.inputId) match {
        case Some(transformTask) =>
          val input = loadInput(transformTask.data.selection, transformTask.data.inputSchema)
          ExecutorRegistry.execute[TransformSpec, ExecutionType](transformTask, Seq(input), Some(entitySchema), execution)
        case None =>
          val datasetTask = task.project.task[GenericDatasetSpec](selection.inputId)
          ExecutorRegistry.execute(datasetTask, Seq.empty, Some(entitySchema), execution)
      }

    result.getOrElse(throw AbortExecutionException(s"The input task ${selection.inputId} did not generate any result"))
  }

}
