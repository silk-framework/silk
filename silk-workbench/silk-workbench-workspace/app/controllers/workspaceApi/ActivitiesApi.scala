package controllers.workspaceApi

import controllers.core.{UserContextActions}
import controllers.core.util.ControllerUtilsTrait

import javax.inject.Inject
import org.silkframework.runtime.activity.{Status => ActivityStatus}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.ActivitySerializers.ExtendedStatusJsonFormat
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}

/**
  * Activities API.
  */
class ActivitiesApi @Inject() () extends InjectedController with UserContextActions with ControllerUtilsTrait {

  /** List status of all task activities. */
  def taskActivitiesStatus(projectId: Option[String],
                           statusFilter: Option[String]): Action[AnyContent] = RequestUserContextAction { implicit request =>implicit userContext =>
    val activityStatusTraversable: Traversable[JsValue] = new Traversable[JsValue] {
      override def foreach[U](f: JsValue => U): Unit = {
        val projects = projectId match {
          case Some(id) =>
            Seq(workspace.project(id))
          case None =>
            workspace.projects
        }
        implicit val writeContext: WriteContext[JsValue] = WriteContext()
        for(project <- projects;
            task <- project.allTasks;
            activity <- task.activities;
            status <- activity.status.get
            if statusMatches(status, statusFilter)) {
          val extendedJsonFormat = new ExtendedStatusJsonFormat(project.config.id, task.id, activity.name, activity.startTime)
          f(extendedJsonFormat.write(status))
        }
      }

      def statusMatches(status: ActivityStatus, statusFilter: Option[String]): Boolean = {
        statusFilter.isEmpty || statusFilter.contains(status.name) || statusFilter.contains(status.concreteStatus)
      }
    }
    Ok(Json.toJson(activityStatusTraversable.toSeq))
  }
}

