package controllers.workspace

import java.util.logging.{LogRecord, Logger}

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Merge, Source}
import controllers.core.{RequestUserContextAction, Stream, UserContextAction, Widgets}
import controllers.util.{ObservableWebSocket, SerializationUtils}
import javax.inject.Inject
import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.{Activity, ActivityControl, UserContext, _}
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException, ValidationException}
import org.silkframework.util.Identifier
import org.silkframework.workbench.utils.ErrorResult
import org.silkframework.workspace.activity.WorkspaceActivity
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.http.ContentTypes
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{JsArray, Json}
import play.api.mvc._
import org.silkframework.serialization.json.ActivitySerializers._
import scala.language.existentials

class ActivityApi @Inject() (implicit system: ActorSystem, mat: Materializer) extends InjectedController {

  def getProjectActivities(projectName: String): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    Ok(JsonSerializer.projectActivities(project))
  }

  def getTaskActivities(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.anyTask(taskName)
    Ok(JsonSerializer.taskActivities(task))
  }

  def startActivity(projectName: String,
                    taskName: String,
                    activityName: String,
                    blocking: Boolean): Action[AnyContent] = RequestUserContextAction { request => implicit userContext: UserContext =>
    val w = WorkspaceFactory.factory.workspace
    val project = w.project(projectName)
    val config = activityConfig(request)
    val activity: WorkspaceActivity[_ <: HasValue] =
      if (taskName.nonEmpty) {
        project.anyTask(taskName).activity(activityName)
      } else {
        project.activity(activityName)
      }

    if (activity.isSingleton && activity.status.isRunning) {
      ErrorResult(BAD_REQUEST, title = "Cannot start activity", detail = s"Cannot start activity '$activityName'. Already running.")
    } else {
      if (blocking) {
        activity.startBlocking(config)
        NoContent
      } else {
        val id = activity.start(config)
        Ok(Json.obj(("activityId", id.toString)))
      }
    }
  }

  def cancelActivity(projectName: String, taskName: String, activityName: String): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    val activity = activityControl(projectName, taskName, activityName)
    activity.cancel()
    Ok
  }

  def restartActivity(projectName: String,
                      taskName: String,
                      activityName: String,
                      blocking: Boolean): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    val activity = activityControl(projectName, taskName, activityName)
    activity.reset()
    if(blocking) {
      activity.startBlocking()
    } else {
      activity.start()
    }
    Ok
  }

  def getActivityConfig(projectName: String, taskName: String, activityName: String): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val activityConfig =
      if (taskName.nonEmpty) {
        val task = project.anyTask(taskName)
        task.activity(activityName).defaultConfig
      } else {
        project.activity(activityName).defaultConfig
      }

    Ok(JsonSerializer.activityConfig(activityConfig))
  }

  def postActivityConfig(projectName: String,
                         taskName: String,
                         activityName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext: UserContext =>
    val config = activityConfig(request)
    if (config.nonEmpty) {
      val project = WorkspaceFactory().workspace.project(projectName)
      if (taskName.nonEmpty) {
        val task = project.anyTask(taskName)
        task.activity(activityName).update(config)
      } else {
        project.activity(activityName).update(config)
      }
      Ok
    } else {
      ErrorResult(BadUserInputException("No config supplied"))
    }
  }

  def getActivityStatus(projectName: String, taskName: String, activityName: String): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    val activity = activityControl(projectName, taskName, activityName)
    Ok(JsonSerializer.activityStatus(projectName, taskName, activityName, activity.status(), activity.startTime))
  }

  def getActivityValue(projectName: String,
                       taskName: String,
                       activityName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext: UserContext =>
    implicit val project: Project = WorkspaceFactory().workspace.project(projectName)
    val activity = activityControl(projectName, taskName, activityName)
    val value = activity.value()
    SerializationUtils.serializeRuntime(value)
  }

  def recentActivities(maxCount: Int): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    // Get all projects and tasks
    val projects = WorkspaceFactory().workspace.projects
    val tasks: Seq[ProjectTask[_]] = projects.flatMap(_.allTasks)

    // Get all activities
    val projectActivities = projects.flatMap(_.activities)
    val taskActivities = tasks.flatMap(_.activities.asInstanceOf[Seq[WorkspaceActivity[_ <: HasValue]]])
    val allActivities = projectActivities ++ taskActivities

    // Filter recent activities
    val recentActivities = allActivities.sortBy(-_.status.timestamp).take(maxCount)

    // Get all statuses
    val statuses = recentActivities.map(JsonSerializer.activityStatus)

    Ok(JsArray(statuses))
  }

  def activityLog(): Action[AnyContent] = Action {
    Ok(JsonSerializer.logRecords(ActivityLog.records))
  }

  def activityUpdates(projectName: String,
                      taskName: String,
                      activityName: String): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    val projects =
      if (projectName.nonEmpty) WorkspaceFactory().workspace.project(projectName) :: Nil
      else WorkspaceFactory().workspace.projects

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

    val allSources = projectActivityStreams ++ taskActivityStreams

    Ok.chunked(Source.combine(Source.empty, Source.empty, allSources: _*)(Merge(_))).as(ContentTypes.HTML)
  }

  def activityUpdatesWebSocket(projectName: String,
                      taskName: String,
                      activityName: String): WebSocket = {

    implicit val userContext = UserContext.Empty

    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.anyTask(taskName)
    val activity = task.activity(activityName)

    ObservableWebSocket.create(activity.control.status)
  }

  private def activityControl(projectName: String, taskName: String, activityName: String)
                             (implicit userContext: UserContext): ActivityControl[_] = {
    val project = WorkspaceFactory().workspace.project(projectName)
    val activityId: Identifier = activityName
    if (taskName.nonEmpty) {
      val task = project.anyTask(taskName)
      val activities = task.activities.flatMap(_.allInstances.get(activityId).asInstanceOf[Option[ActivityControl[_]]].toSeq)
      activities match {
        case Seq(activity) => activity
        case Seq() => throw new NotFoundException(s"Activity with id $activityName not found")
        case _ => throw new ValidationException(s"Multiple activities with id $activityName found")
      }
    } else {
      project.activity(activityName).control
    }
  }

  /** Only affects activities with singleton==false setting, removes activity control instance */
  def removeActivityControl(projectName: String,
                            taskName: String,
                            activityName: String): Action[AnyContent] = UserContextAction { implicit userContext: UserContext =>
    val activity = activityControl(projectName, taskName, activityName)
    activity.cancel()
    Ok
  }

  private def removeActivityControl(projectName: String, taskName: String, activityName: String)
                                   (implicit userContext: UserContext): Unit = {
    val project = WorkspaceFactory().workspace.project(projectName)
    val activityId: Identifier = activityName
    if (taskName.nonEmpty) {
      val task = project.anyTask(taskName)
      task.activities.foreach(_.removeActivityInstance(activityId))
    } else {
      project.activity(activityName).removeActivityInstance(activityId)
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
