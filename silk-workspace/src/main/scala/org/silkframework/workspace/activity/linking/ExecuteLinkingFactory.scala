package org.silkframework.workspace.activity.linking

import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.{AbortExecutionException, ExecutionType, ExecutorOutput, ExecutorRegistry}
import org.silkframework.rule.execution.ComparisonToRestrictionConverter
import org.silkframework.rule.{DatasetSelection, LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.{Activity, ActivityContext, ActivityMonitor, UserContext}
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.DPair
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

  private val comparisonToRestrictionConverter = new ComparisonToRestrictionConverter()

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
    val inputs = loadInputs()

    // Generate links
    context.status.update("Generating links", 0.4)
    val links = ExecutorRegistry.execute(task, inputs, ExecutorOutput.empty, execution,
      new ActivityMonitor(getClass.getSimpleName, projectAndTaskId = context.status.projectAndTaskId)) match {
      case Some(result) => result
      case None => throw AbortExecutionException("Linking task did not generate any links")
    }

    // Write links to outputs
    context.status.update("Writing links to output", 0.8)
    for(output <- task.data.outputs) {
      val outputTask = task.project.task[GenericDatasetSpec](output)
      outputTask.data.linkSink.clear() // Clear link sink before writing in single execution mode
      ExecutorRegistry.execute(outputTask, Seq(links), ExecutorOutput.empty, execution)
    }
  }

  private def loadInputs()
                        (implicit executionType: ExecutionType,
                         userContext: UserContext): DPair[ExecutionType#DataType] = {
    for (((selection, schema), sourceOrTarget) <- task.data.dataSelections zip task.data.entityDescriptions zip Seq(true, false)) yield {
      loadInput(selection, schema, Some(sourceOrTarget))
    }
  }

  /** @param sourceOrTarget Is this the source or target input of the link spec. This should be None if this is not a direct input.
    *                       This is needed for optional SPARQL restriction generation from the linkage rule. */
  private def loadInput(selection: DatasetSelection,
                        entitySchema: EntitySchema,
                        sourceOrTarget: Option[Boolean])
                       (implicit execution: ExecutionType,
                        userContext: UserContext): ExecutionType#DataType = {
    val updatedEntitySchema = sourceOrTarget.map(sot =>
      comparisonToRestrictionConverter.extendEntitySchemaWithLinkageRuleRestriction(entitySchema, task.data.rule, sot)
    ).getOrElse(entitySchema)
    val result =
      task.project.taskOption[TransformSpec](selection.inputId) match {
        case Some(transformTask) =>
          val input = loadInput(transformTask.data.selection, transformTask.data.inputSchema, None)
          ExecutorRegistry.execute[TransformSpec, ExecutionType](transformTask, Seq(input),
            ExecutorOutput(None, Some(entitySchema)), execution)
        case None =>
          val datasetTask = task.project.task[GenericDatasetSpec](selection.inputId)
          ExecutorRegistry.execute(datasetTask, Seq.empty, ExecutorOutput(None, Some(updatedEntitySchema)), execution)
      }

    result.getOrElse(throw AbortExecutionException(s"The input task ${selection.inputId} did not generate any result"))
  }
}
