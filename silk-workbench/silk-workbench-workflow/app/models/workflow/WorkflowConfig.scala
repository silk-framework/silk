package models.workflow

import org.silkframework.config.{DefaultConfig, ProductionConfig}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.util.Identifier
import org.silkframework.workbench.WorkbenchPlugin.TaskActions
import org.silkframework.workspace.activity.workflow.Workflow

object WorkflowConfig {

  /**
    * Returns the workflow execution activity that corresponds to the configured execution manager.
    */
  def executorName: String = {
    DefaultConfig.instance().getString("execution.manager.plugin") match {
      case "LocalExecutionManager" => "ExecuteLocalWorkflow"
      case "SparkExecutionManager" => "ExecuteSparkWorkflow"
      case manager: String =>
        throw new RuntimeException("Unknown execution manager: " + manager)
    }
  }

  /**
    * Given a task in the workflow, returns the URI of the corresponding details page.
    */
  def editorLink(taskActions: TaskActions, workflowId: Identifier): Option[String] = {
    if(ProductionConfig.betaWorkspaceSwitchEnabled) {
      val projectId = taskActions.task.project.name
      val taskId = taskActions.task.id
      val taskType = taskActions.task.data match {
        case _: GenericDatasetSpec => "dataset"
        case _: TransformSpec => "transform"
        case _: LinkSpec => "linking"
        case _: Workflow => "workflow"
        case _ => "task"
      }
      Some(s"${config.baseUrl}/workbench/projects/$projectId/$taskType/$taskId")
    } else {
      for(path <- taskActions.openPath(Some(workflowId), Some(taskActions.task.id.toString))) yield {
        s"${config.baseUrl}/$path"
      }
    }
  }

}
