package org.silkframework.workspace

import org.silkframework.config.TaskSpec
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.workspace.activity.workflow.Workflow

object WorkbenchLinks {

  def editorLink(task: ProjectTask[_ <: TaskSpec]): String = {
    val projectId = task.project.id
    val taskId = task.id
    s"/workbench/projects/$projectId/${taskType(task)}/$taskId"
  }

  def taskType(task: ProjectTask[_ <: TaskSpec]): String = {
    task.data match {
      case _: GenericDatasetSpec => "dataset"
      case _: TransformSpec      => "transform"
      case _: LinkSpec           => "linking"
      case _: Workflow           => "workflow"
      case _                     => "task"
    }
  }
}
