package config

import org.silkframework.config.{ProductionConfig, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.util.Identifier
import org.silkframework.workbench.WorkbenchPlugin.TaskActions
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.workflow.Workflow

object WorkbenchLinks {

  /**
    * Given a task in the workflow, returns the URI of the corresponding details page.
    */
  def editorLink(taskActions: TaskActions, workflowId: Identifier): Option[String] = {
    if(ProductionConfig.betaWorkspaceSwitchEnabled) {
      Some(editorLink(taskActions.task))
    } else {
      for(path <- taskActions.openPath(Some(workflowId), Some(taskActions.task.id.toString))) yield {
        s"${config.baseUrl}/$path"
      }
    }
  }

  /**
    * Given a task in the workflow, returns the URI of the corresponding details page in the new workspace.
    */
  def editorLink(task: ProjectTask[_ <: TaskSpec]): String = {
    val projectId = task.project.name
    val taskId = task.id
    val taskType = task.data match {
      case _: GenericDatasetSpec => "dataset"
      case _: TransformSpec => "transform"
      case _: LinkSpec => "linking"
      case _: Workflow => "workflow"
      case _ => "task"
    }
    s"${config.baseUrl}/workbench/projects/$projectId/$taskType/$taskId"
  }

}
