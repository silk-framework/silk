package controllers.workspaceApi

import controllers.core.RequestUserContextAction
import controllers.core.util.ControllerUtilsTrait
import controllers.workspaceApi.activities.TaskActivityStatus
import javax.inject.Inject
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}
import org.silkframework.runtime.activity.{Status => ActivityStatus}

/**
  * Activities API.
  */
class ActivitiesApi @Inject() () extends InjectedController with ControllerUtilsTrait {

  /** List status of all task activities. */
  def taskActivitiesStatus(projectId: Option[String],
                           statusFilter: Option[String]): Action[AnyContent] = RequestUserContextAction { implicit request =>implicit userContext =>
    val activityTraversable: Traversable[TaskActivityStatus] = new Traversable[TaskActivityStatus] {
      override def foreach[U](f: TaskActivityStatus => U): Unit = {
        val projects = projectId match {
          case Some(id) =>
            Seq(workspace.project(id))
          case None =>
            workspace.projects
        }
        for(project <- projects;
            task <- project.allTasks;
            activity <- task.activities;
            status <- activity.status.get
            if statusMatches(status, statusFilter)) {
          f(TaskActivityStatus(
            projectId = project.config.id,
            taskId = task.id,
            activityId = activity.name,
            startTime = activity.startTime,
            concreteStatus = status.concreteStatus,
            statusDetails = status
          ))
        }
      }

      def statusMatches(status: ActivityStatus, statusFilter: Option[String]): Boolean = {
        statusFilter.isEmpty || statusFilter.contains(status.name) || statusFilter.contains(status.concreteStatus)
      }
    }
    Ok(Json.toJson(activityTraversable))
  }
}

