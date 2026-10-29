package controllers.workflowApi.workflow

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json.{Json, Reads, Writes}

case class DependentTasksInWorkflowRequest(projectId: String,
                                           taskId: String,
                                           workflowId: String) {

  def apply()(implicit user: UserContext): DependentTasksInWorkflowResponse = {
    val project = WorkspaceFactory().workspace.project(projectId)
    val workflow = project.task[Workflow](workflowId)

    val dependentTasks =
      for {
        id <- findDependencies(project, workflow, taskId, Set.empty)
        if (id != taskId || workflow.referencedTasks.contains(Identifier(taskId))) && id != workflowId
      } yield {
        val task = project.anyTask(id)
        TaskIdAndLabel(task.id.toString, task.fullLabel)
      }

    DependentTasksInWorkflowResponse(dependentTasks.toArray)
  }

  private def findDependencies(project: Project, workflowDependency: ProjectTask[_ <: TaskSpec], taskId: Identifier,
                               visited: Set[Identifier])(implicit user: UserContext): Set[String] = {
    // Track the tasks on the current path, so that a reference cycle does not lead to endless recursion
    val newVisited = visited + workflowDependency.id
    val allReferencedTasks =
      for {
        referencedTaskId <- workflowDependency.referencedTasks
        if !newVisited.contains(referencedTaskId)
        referencedTask = project.anyTask(referencedTaskId)
        dependentTask <- findDependencies(project, referencedTask, taskId, newVisited)
      } yield {
        dependentTask
      }

    if(workflowDependency.id == taskId) {
      Set(taskId)
    } else if(allReferencedTasks.contains(taskId)) {
      allReferencedTasks + workflowDependency.id
    } else {
      Set.empty
    }
  }

}

object DependentTasksInWorkflowRequest {
  implicit val format: Reads[DependentTasksInWorkflowRequest] = Json.reads[DependentTasksInWorkflowRequest]
}

case class DependentTasksInWorkflowResponse(tasks: Array[TaskIdAndLabel])

case class TaskIdAndLabel(taskId: String,
                          label: String)

object TaskIdAndLabel {
  implicit val format: Writes[TaskIdAndLabel] = Json.writes[TaskIdAndLabel]
}

object DependentTasksInWorkflowResponse {
  implicit val format: Writes[DependentTasksInWorkflowResponse] = Json.writes[DependentTasksInWorkflowResponse]
}
