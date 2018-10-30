package org.silkframework.workspace.activity.workflow

import org.silkframework.dataset.Dataset
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory

@Plugin(
  id = "ExecuteWorkflowWithPayload",
  label = "Execute Workflow with payload",
  categories = Array("WorkflowExecution"),
  description = "Executes a workflow with custom payload."
)
class WorkflowWithPayloadExecutorFactory extends TaskActivityFactory[Workflow, WorkflowWithPayloadExecutor] {

  def apply(task: ProjectTask[Workflow]): Activity[WorkflowPayload] = {
    new WorkflowWithPayloadExecutor(task)
  }
}

class WorkflowWithPayloadExecutor(task: ProjectTask[Workflow]) extends Activity[WorkflowPayload] {

  override def run(context: ActivityContext[WorkflowPayload])
                  (implicit userContext: UserContext): Unit = {
    val payload = context.value()
    val activity = LocalWorkflowExecutor(task, payload.dataSources, payload.dataSinks, useLocalInternalDatasets = true)
    context.child(activity, 1.0)
  }
}

case class WorkflowPayload(dataSources: Map[String, Dataset], dataSinks: Map[String, Dataset])
