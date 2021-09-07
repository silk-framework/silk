package controllers.workspace.activityApi

import controllers.workspace.activityApi.ActivityListResponse.{ActivityCharacteristics, ActivityInstance, ActivityListEntry}
import org.silkframework.runtime.activity.{HasValue, UserContext}
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.WorkspaceActivity

/**
  * Provides a simple API for accessing and controlling activities.
  */
object ActivityFacade {

  def listActivities(projectName: String,
                     taskName: String)
                    (implicit user: UserContext): Seq[ActivityListEntry] = {
    var mainActivities: Seq[String] = Seq.empty
    val activities =
      if(projectName.nonEmpty) {
        val project = WorkspaceFactory().workspace.project(projectName)
        if(taskName.nonEmpty) {
          val task = project.anyTask(taskName)
          mainActivities = task.data.mainActivities
          task.activities
        } else {
          project.activities
        }
      } else {
        WorkspaceFactory().workspace.activities
      }

    for(activity <- activities) yield {
      ActivityListEntry(
        name = activity.name.toString,
        instances = activity.allInstances.keys.toSeq.map(id => ActivityInstance(id.toString)),
        activityCharacteristics = ActivityCharacteristics(
          isMainActivity = mainActivities.contains(activity.name.toString),
          isCacheActivity = activity.isCacheActivity
        )
      )
    }
  }

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
