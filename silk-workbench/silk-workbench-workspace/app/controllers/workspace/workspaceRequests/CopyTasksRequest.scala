package controllers.workspace.workspaceRequests

import io.swagger.v3.oas.annotations.media.Schema
import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json.{Json, OFormat}

/**
  * Request to copy a project or a task to another project.
  */
case class CopyTasksRequest(@Schema(
                              description = "If true, the copy operation will be simulated, i.e., the response listing the tasks to be copied and overwritten can be checked first.",
                              required = false,
                              nullable = true
                            )
                            dryRun: Option[Boolean],
                            @Schema(
                              description = "If true, tasks in the target project will be overwritten.",
                              required = false,
                              nullable = true
                            )
                            overwriteTasks: Option[Boolean],
                            @Schema(
                              description = "The identifier of the target project."
                            )
                            targetProject: String) {

  private def isDryRun: Boolean = dryRun.contains(true)
  private def overwriteConfirmed: Boolean = overwriteTasks.contains(true)

  /**
    * Copies all tasks in a project to the target project.
    */
  def copyProject(sourceProject: String)
                 (implicit userContext: UserContext): CopyTasksResponse = {
    val sourceProj = WorkspaceFactory().workspace.project(sourceProject)
    val tasksToCopy = sourceProj.allTasks
    copyTasks(sourceProj, tasksToCopy)
  }

  /**
    * Copies a task and all its referenced tasks to the target project.
    */
  def copyTask(sourceProject: String, taskName: String)
              (implicit userContext: UserContext): CopyTasksResponse = {
    val sourceProj = WorkspaceFactory().workspace.project(sourceProject)
    val tasksToCopy = collectTasks(sourceProj, taskName)
    copyTasks(sourceProj, tasksToCopy)
  }

  /**
    * Copies all provided tasks to the target project.
    */
  private def copyTasks(sourceProj: Project, tasksToCopy: Seq[ProjectTask[_ <:TaskSpec]])
                       (implicit userContext: UserContext): CopyTasksResponse = {
    val targetProj = WorkspaceFactory().workspace.project(targetProject)

    sourceProj.synchronized {
      targetProj.synchronized {
        // Only copy resources if they are at different base paths
        val copyResources = sourceProj.resources.basePath != targetProj.resources.basePath

        // Tasks to be overwritten
        val overwrittenTasks =
          for { task <- tasksToCopy
                overwrittenTask <- targetProj.anyTaskOption(task.id) } yield TaskToBeCopied.fromTask(task, Some(overwrittenTask))

        // Copy tasks
        if(!isDryRun) {
          if(overwrittenTasks.nonEmpty && !overwriteConfirmed) {
            throw BadUserInputException("Please confirm that you intend to overwrite tasks in the target project.")
          }

          for (task <- tasksToCopy) {
            targetProj.updateAnyTask(task.id, task.data, Some(task.metaData))
            // Copy resources
            if(copyResources) {
              for (resource <- task.referencedResources if resource.exists) {
                targetProj.resources.get(resource.name).writeResource(resource)
              }
            }
          }
        }

        // Generate response
        val overwrittenTaskIds = overwrittenTasks.map(_.id).toSet
        val copiedTasks = for(task <- tasksToCopy if !overwrittenTaskIds.contains(task.id)) yield TaskToBeCopied.fromTask(task, None)
        CopyTasksResponse(copiedTasks.toSet, overwrittenTasks.toSet)
      }
    }
  }

  /**
    * Returns a task and all its referenced tasks.
    */
  private def collectTasks(project: Project, taskName: Identifier)
                          (implicit userContext: UserContext): Seq[ProjectTask[_ <:TaskSpec]] = {
    val task = project.anyTask(taskName)
    Seq(task) ++ task.data.referencedTasks.flatMap(collectTasks(project, _))
  }

}

object CopyTasksRequest {

  implicit val jsonFormat: OFormat[CopyTasksRequest] = Json.format[CopyTasksRequest]

}