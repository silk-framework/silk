package controllers.workspace.activityApi

import org.silkframework.runtime.activity.{HasValue, UserContext}
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.WorkspaceActivity

object ActivityUtils {

  def start(projectName: String,
            taskName: String,
            activityName: String,
            blocking: Boolean,
            activityConfig: Map[String, String])
           (implicit user: UserContext): StartActivityResponse = {
    val activity = getActivity(projectName, taskName, activityName)
    if (activity.isSingleton && activity.status().isRunning) {
      throw ActivityAlreadyRunningException(activityName)
    } else {
      val id =
        if (blocking) {
          activity.startBlocking(activityConfig)
        } else {
          activity.start(activityConfig)
        }
      StartActivityResponse(id.toString)
    }
  }

  def getActivity(projectName: String,
                  taskName: String,
                  activityName: String)
                 (implicit user: UserContext): WorkspaceActivity[_ <: HasValue] = {
    val workspace = WorkspaceFactory.factory.workspace
    if(projectName.trim.isEmpty) {
      workspace.activity(activityName)
    } else {
      val project = workspace.project(projectName)
      if (taskName.nonEmpty) {
        project.anyTask(taskName).activity(activityName)
      } else {
        project.activity(activityName)
      }
    }
  }

}
