package controllers.workspace

import java.util.logging.{LogRecord, Logger}

import controllers.core.{Stream, Widgets}
import models.JsonError
import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.{Activity, ActivityControl}
import org.silkframework.runtime.serialization.Serialization
import org.silkframework.workspace.activity.WorkspaceActivity
import org.silkframework.workspace.{Project, ProjectTask, User}
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsArray
import play.api.mvc._
import scala.language.existentials

object ActivityApi extends Controller {

  def getProjectActivities(projectName: String) = Action {
    val project = User().workspace.project(projectName)
    Ok(JsonSerializer.projectActivities(project))
  }

  def getTaskActivities(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.anyTask(taskName)
    Ok(JsonSerializer.taskActivities(task))
  }

  def startActivity(projectName: String, taskName: String, activityName: String, blocking: Boolean) = Action { request =>
    val project = User().workspace.project(projectName)
    val config = activityConfig(request)
    val activityControl =
      if (taskName.nonEmpty) {
        val activity = project.anyTask(taskName).activity(activityName)
        if (config.nonEmpty)
          activity.update(config)
        activity.control
      } else {
        val activity = project.activity(activityName)
        if (config.nonEmpty)
          activity.update(config)
        activity.control
      }

    if (activityControl.status().isRunning) {
      BadRequest(JsonError(s"Cannot start activity '$activityName'. Already running."))
    } else {
      if (blocking)
        activityControl.startBlocking()
      else
        activityControl.start()
      Ok
    }
  }

  def cancelActivity(projectName: String, taskName: String, activityName: String) = Action {
    val activity = activityControl(projectName, taskName, activityName)
    activity.cancel()
    Ok
  }

  def restartActivity(projectName: String, taskName: String, activityName: String) = Action {
    val activity = activityControl(projectName, taskName, activityName)
    activity.reset()
    activity.start()
    Ok
  }

  def getActivityConfig(projectName: String, taskName: String, activityName: String) = Action {
    val project = User().workspace.project(projectName)
    val activityConfig =
      if (taskName.nonEmpty) {
        val task = project.anyTask(taskName)
        task.activity(activityName).config
      } else {
        project.activity(activityName).config
      }

    Ok(JsonSerializer.activityConfig(activityConfig))
  }

  def postActivityConfig(projectName: String, taskName: String, activityName: String) = Action { request =>
    val config = activityConfig(request)
    if (config.nonEmpty) {
      val project = User().workspace.project(projectName)
      if (taskName.nonEmpty) {
        val task = project.anyTask(taskName)
        task.activity(activityName).update(config)
      } else {
        project.activity(activityName).update(config)
      }
      Ok
    } else {
      BadRequest(JsonError("No config supplied."))
    }
  }

  def getActivityStatus(projectName: String, taskName: String, activityName: String) = Action {
    val project = User().workspace.project(projectName)
    if (taskName.nonEmpty) {
      val task = project.anyTask(taskName)
      val activity = task.activity(activityName)
      Ok(JsonSerializer.activityStatus(projectName, taskName, activityName, activity.status))
    } else {
      val activity = project.activity(activityName)
      Ok(JsonSerializer.activityStatus(projectName, taskName, activityName, activity.status))
    }
  }

  def getActivityValue(projectName: String, taskName: String, activityName: String) = Action { request =>
    val activity = activityControl(projectName, taskName, activityName)
    val value = activity.value()

    val mimeTypes = request.acceptedTypes.map(t => t.mediaType + "/" + t.mediaSubType)
    if (mimeTypes.isEmpty) {
      // If no MIME type has been specified, we return XML
      val serializeValue = Serialization.serialize(value, "application/xml")
      Ok(serializeValue).as("application/xml")
    } else {
      mimeTypes.find(Serialization.hasSerialization(value, _)) match {
        case Some(mimeType) =>
          val serializeValue = Serialization.serialize(value, mimeType)
          Ok(serializeValue).as(mimeType)
        case None =>
          NotAcceptable(value.toString)
      }
    }
  }

  def recentActivities(maxCount: Int) = Action {
    // Get all projects and tasks
    val projects = User().workspace.projects
    val tasks: Seq[ProjectTask[_]] = projects.flatMap(_.allTasks)

    // Get all activities
    val projectActivities = projects.flatMap(_.activities)
    val taskActivities = tasks.flatMap(_.activities.asInstanceOf[Seq[WorkspaceActivity]])
    val allActivities = projectActivities ++ taskActivities

    // Filter recent activities
    val recentActivities = allActivities.sortBy(-_.status.timestamp).take(maxCount)

    // Get all statuses
    val statuses = recentActivities.map(JsonSerializer.activityStatus)

    Ok(JsArray(statuses))
  }

  def activityLog() = Action {
    Ok(JsonSerializer.logRecords(ActivityLog.records))
  }

  def activityUpdates(projectName: String, taskName: String, activityName: String) = Action {
    val projects =
      if (projectName.nonEmpty) User().workspace.project(projectName) :: Nil
      else User().workspace.projects

    def tasks(project: Project) =
      if (taskName.nonEmpty) project.anyTask(taskName) :: Nil
      else project.allTasks

    def projectActivities(project: Project) =
      if (taskName.nonEmpty) Nil
      else project.activities

    def taskActivities(task: ProjectTask[_ <: TaskSpec]) =
      if (activityName.nonEmpty) task.activity(activityName) :: Nil
      else task.activities

    val projectActivityStreams =
      for (project <- projects; activity <- projectActivities(project)) yield
        Widgets.statusStream(Enumerator(activity.status) andThen Stream.status(activity.control.status), project = project.name, task = "", activity = activity.name)

    val taskActivityStreams =
      for (project <- projects;
           task <- tasks(project);
           activity <- taskActivities(task)) yield
        Widgets.statusStream(Enumerator(activity.status) andThen Stream.status(activity.control.status), project = project.name, task = task.id, activity = activity.name)

    Ok.chunked(Enumerator.interleave(projectActivityStreams ++ taskActivityStreams))
  }

  private def activityControl(projectName: String, taskName: String, activityName: String): ActivityControl[_] = {
    val project = User().workspace.project(projectName)
    if (taskName.nonEmpty) {
      val task = project.anyTask(taskName)
      task.activity(activityName).control
    } else {
      project.activity(activityName).control
    }
  }

  private def activityConfig(request: Request[AnyContent]): Map[String, String] = {
    request.body match {
      case AnyContentAsFormUrlEncoded(values) =>
        values.mapValues(_.head)
      case _ =>
        request.queryString.mapValues(_.head)
    }
  }
}

/**
  * Holds the activities log.
  */
object ActivityLog extends java.util.logging.Handler {

  private val size = 100

  private val buffer = Array.fill[LogRecord](size)(null)

  private var start = 0

  private var count = 0

  private val log = Logger.getLogger(getClass.getName)

  private val activitiesLogger = Logger.getLogger(Activity.loggingPath)

  init()

  def init(): Unit = {
    activitiesLogger.addHandler(this)
    log.fine("Logging of activities started.")
  }

  /**
    * Retrieves the recent log records
    */
  def records: Seq[LogRecord] = synchronized {
    log.fine(s"Retrieving $count activity logs")
    for (i <- 0 until count) yield {
      buffer((start + i) % buffer.length)
    }
  }

  /**
    * Adds a new log record.
    */
  override def publish(record: LogRecord): Unit = synchronized {
    log.fine(s"Adding activities log for '${record.getLoggerName}'")
    val ix = (start + count) % buffer.length
    buffer(ix) = record
    if (count < buffer.length) {
      count += 1
    }
    else {
      start += 1
      start %= buffer.length
    }
  }

  override def flush(): Unit = {}

  override def close(): Unit = {}
}
