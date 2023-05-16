package controllers.workspace.workspaceRequests

import io.swagger.v3.oas.annotations.media.Schema
import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
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
    val targetProj = WorkspaceFactory().workspace.project(targetProject)
    CopyTasksRequest.copyTasks(sourceProj, tasksToCopy, targetProj, isDryRun, overwriteConfirmed)
  }

  def copyTask(sourceProject: String,
               taskName: String)
              (implicit userContext: UserContext): CopyTasksResponse = {
    CopyTasksRequest.copyTask(sourceProject, taskName, targetProject, isDryRun, overwriteConfirmed)
  }
}

object CopyTasksRequest {

  implicit val jsonFormat: OFormat[CopyTasksRequest] = Json.format[CopyTasksRequest]

  /**
    * Copies a task and all its referenced tasks to the target project.
    * @param sourceProject      The source project ID.
    * @param taskName           The task that should be copied from the source project to the target project.
    * @param targetProject      The target project ID.
    * @param isDryRun           Only return the response as if the task was copied, but do not actually do any changes.
    * @param overwriteConfirmed If true, then existing tasks will be overwritten.
    * @param taskRenameMap      Specifies how some tasks should be named in the target project.
    */
  def copyTask(sourceProject: String,
               taskName: String,
               targetProject: String,
               isDryRun: Boolean,
               overwriteConfirmed: Boolean,
               taskRenameMap: Map[Identifier, Identifier] = Map.empty)
              (implicit userContext: UserContext): CopyTasksResponse = {
    val sourceProj = WorkspaceFactory().workspace.project(sourceProject)
    val tasksToCopy = collectTasks(sourceProj, taskName)
    val targetProj = WorkspaceFactory().workspace.project(targetProject)
    copyTasks(sourceProj, tasksToCopy, targetProj, isDryRun, overwriteConfirmed, taskRenameMap)
  }

  /**
    * Copies all provided tasks to the target project.
    */
  private def copyTasks(sourceProj: Project,
                        tasksToCopy: Seq[ProjectTask[_ <: TaskSpec]],
                        targetProject: Project,
                        isDryRun: Boolean,
                        overwriteConfirmed: Boolean,
                        taskRenameMap: Map[Identifier, Identifier] = Map.empty)
                       (implicit userContext: UserContext): CopyTasksResponse = {
    sourceProj.synchronized {
      targetProject.synchronized {
        // Only copy resources if they are at different base paths
        val copyResources = sourceProj.resources.basePath != targetProject.resources.basePath

        // Copy only those tags that do not exist in the target project
        val targetProjectTags = targetProject.tagManager.allTags().map(_.uri).toSet
        val tagsToCopy = tasksToCopy.flatMap(_.metaData.tags)
          .filter(tag => !targetProjectTags.contains(tag.uri))
          .map(tagUri => sourceProj.tagManager.getTag(tagUri.uri))
        for (tag <- tagsToCopy) {
          targetProject.tagManager.putTag(tag)
        }

        // Tasks to be overwritten
        val overwrittenTasks =
          for {task <- tasksToCopy
               overwrittenTask <- targetProject.anyTaskOption(taskRenameMap.getOrElse(task.id, task.id))} yield TaskToBeCopied.fromTask(task, Some(overwrittenTask))

        // Copy tasks
        if (!isDryRun) {
          if (overwrittenTasks.nonEmpty && !overwriteConfirmed) {
            throw BadUserInputException("Please confirm that you intend to overwrite tasks in the target project.")
          }

          for (task <- tasksToCopy) {
            val taskParameters = task.data.parameters(PluginContext.fromProject(sourceProj))
            val clonedTaskSpec = task.data.withParameters(taskParameters, dropExistingValues = true)(PluginContext.fromProject(targetProject))
            targetProject.updateAnyTask(taskRenameMap.getOrElse(task.id, task.id), clonedTaskSpec, Some(task.metaData))
            // Copy resources
            if (copyResources) {
              for (resource <- task.referencedResources if resource.exists) {
                targetProject.resources.get(resource.name).writeResource(resource)
              }
            }
          }
        }

        // Generate response
        val overwrittenTaskIds = overwrittenTasks.map(_.id).toSet
        val copiedTasks = for (task <- tasksToCopy if !overwrittenTaskIds.contains(task.id)) yield TaskToBeCopied.fromTask(task, None)
        CopyTasksResponse(copiedTasks.toSet, overwrittenTasks.toSet)
      }
    }
  }

  /**
    * Returns a task and all its referenced tasks.
    */
  private def collectTasks(project: Project, taskName: Identifier)
                          (implicit userContext: UserContext): Seq[ProjectTask[_ <: TaskSpec]] = {
    val task = project.anyTask(taskName)
    Seq(task) ++ task.data.referencedTasks.flatMap(collectTasks(project, _))
  }

}