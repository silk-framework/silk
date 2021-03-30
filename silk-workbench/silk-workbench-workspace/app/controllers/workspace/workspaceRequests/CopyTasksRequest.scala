package controllers.workspace.workspaceRequests

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, WorkspaceFactory}

/**
  * Request to copy a project or a task to another project.
  */
case class CopyTasksRequest(dryRun: Option[Boolean], targetProject: String) {

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
  private def copyTasks(sourceProj: Project, tasksToCopy: Seq[Task[_ <:TaskSpec]])
                       (implicit userContext: UserContext): CopyTasksResponse = {
    val targetProj = WorkspaceFactory().workspace.project(targetProject)

    sourceProj.synchronized {
      targetProj.synchronized {

        val overwrittenTasks = for(task <- tasksToCopy if targetProj.anyTaskOption(task.id).isDefined) yield task.id.toString
        val copyResources = sourceProj.resources.basePath != targetProj.resources.basePath

        // Copy tasks
        if(!dryRun.contains(true)) {
          for (task <- tasksToCopy) {
            targetProj.updateAnyTask(task.id, task.data, Some(task.metaData))
            // Copy resources
            if(copyResources) {
              for (resource <- task.referencedResources) {
                targetProj.resources.get(resource.name).writeResource(resource)
              }
            }
          }
        }

        // Generate response
        CopyTasksResponse(tasksToCopy.map(_.id.toString).toSet, overwrittenTasks.toSet)
      }
    }
  }

  /**
    * Returns a task and all its referenced tasks.
    */
  private def collectTasks(project: Project, taskName: Identifier)
                          (implicit userContext: UserContext): Seq[Task[_ <:TaskSpec]] = {
    val task = project.anyTask(taskName)
    Seq(task) ++ task.data.referencedTasks.flatMap(collectTasks(project, _))
  }

}