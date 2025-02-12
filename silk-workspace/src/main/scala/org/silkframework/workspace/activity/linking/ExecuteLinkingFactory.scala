package org.silkframework.workspace.activity.linking

import org.silkframework.config.{FixedSchemaPort, Prefixes}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.entity.EntitySchema
import org.silkframework.execution._
import org.silkframework.rule.execution.{ComparisonToRestrictionConverter, Linking}
import org.silkframework.rule.{DatasetSelection, LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.util.DPair
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory

@Plugin(
  id = ExecuteLinkingFactory.pluginId,
  label = "Execute linking",
  categories = Array("LinkSpecification"),
  description = "Executes the linking task using the configured execution."
)
case class ExecuteLinkingFactory() extends TaskActivityFactory[LinkSpec, ExecuteLinking] {
  /**
    * Generates a new activity for a given task.
    */
  override def apply(task: ProjectTask[LinkSpec]): Activity[ExecutionReport] = {
    new ExecuteLinking(task)
  }
}

object ExecuteLinkingFactory {

  final val pluginId = "ExecuteLinking"

}

class ExecuteLinking(task: ProjectTask[LinkSpec]) extends Activity[ExecutionReport] {

  private val comparisonToRestrictionConverter = new ComparisonToRestrictionConverter()

  implicit val prefixes: Prefixes = task.project.config.prefixes

  override val initialValue: Option[Linking] = Some(Linking(task, task.rule))

  /**
    * Executes this activity.
    *
    * @param context Holds the context in which the activity is executed.
    */
  override def run(context: ActivityContext[ExecutionReport])
                  (implicit userContext: UserContext): Unit = {
    implicit val execution: ExecutionType = ExecutorRegistry.execution()
    implicit val pluginContext: PluginContext = PluginContext.fromProject(task.project)

    // Execute inputs
    context.status.updateMessage("Loading inputs")
    val inputs = loadInputs()

    // Generate links
    context.status.updateMessage("Generating links")
    val links = ExecutorRegistry.execute(task, inputs, ExecutorOutput.empty, execution, context) match {
      case Some(result) => result
      case None => throw AbortExecutionException("Linking task did not generate any links")
    }

    // Write links to outputs
    context.status.updateMessage("Writing links to output")
    for(output <- task.data.output) {
      val outputTask = task.project.task[GenericDatasetSpec](output)
      outputTask.data.linkSink.clear() // Clear link sink before writing in single execution mode
      ExecutorRegistry.execute(outputTask, Seq(links), ExecutorOutput.empty, execution)
    }
  }

  private def loadInputs()
                        (implicit executionType: ExecutionType,
                         pluginContext: PluginContext, userContext: UserContext): DPair[ExecutionType#DataType] = {
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
                        pluginContext: PluginContext, userContext: UserContext): ExecutionType#DataType = {
    val updatedEntitySchema = sourceOrTarget.map(sot =>
      comparisonToRestrictionConverter.extendEntitySchemaWithLinkageRuleRestriction(entitySchema, task.data.rule, sot)
    ).getOrElse(entitySchema)
    val result =
      task.project.taskOption[TransformSpec](selection.inputId) match {
        case Some(transformTask) =>
          val input = loadInput(transformTask.data.selection, transformTask.data.inputSchema, None)
          ExecutorRegistry.execute[TransformSpec, ExecutionType](transformTask, Seq(input),
            ExecutorOutput(None, FixedSchemaPort(entitySchema)), execution)
        case None =>
          val datasetTask = task.project.task[GenericDatasetSpec](selection.inputId)
          ExecutorRegistry.execute(datasetTask, Seq.empty, ExecutorOutput(None, FixedSchemaPort(updatedEntitySchema)), execution)
      }

    result.getOrElse(throw AbortExecutionException(s"The input task ${selection.inputId} did not generate any result"))
  }
}
