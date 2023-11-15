package org.silkframework.workspace.activity.workflow

import org.silkframework.config.Task
import org.silkframework.execution.local.{LocalEntities, LocalExecution}
import org.silkframework.execution.{ExecutionReport, Executor, ExecutorOutput}
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.workspace.ProjectTask

/**
  * Executes a workflow as an operator in another workflow.
  */
class LocalWorkflowAsTaskExecutor extends Executor[Workflow, LocalExecution] {

  override def execute(task: Task[Workflow],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    implicit val user: UserContext = pluginContext.user
    val projectTask = task match {
      case pt: ProjectTask[Workflow] => pt
      case _ => throw new IllegalArgumentException("Workflow has to be executed in a project context.")
    }
    val workflowContext = context.asInstanceOf[ActivityContext[WorkflowExecutionReport]]
    workflowContext.value() = WorkflowExecutionReport(task)

    LocalWorkflowExecutor(
      projectTask,
      clearDatasets = false,
      replaceDataSources = execution.replaceDataSources,
      replaceSinks = execution.replaceSinks
    ).run(workflowContext)

    None
  }
}
