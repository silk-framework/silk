package controllers.workspace

import java.io.{ByteArrayOutputStream, FileInputStream}
import java.net.URL
import java.util.logging.{LogRecord, Logger}

import controllers.core.{Stream, Widgets}
import models.JsonError
import org.silkframework.config._
import org.silkframework.runtime.activity.{Activity, ActivityControl}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.{ResourceNotFoundException, EmptyResourceManager, UrlResource}
import org.silkframework.runtime.serialization.{ReadContext, Serialization, XmlSerialization}
import org.silkframework.workspace.activity.{ProjectExecutor, WorkspaceActivity}
import org.silkframework.workspace.io.{SilkConfigExporter, SilkConfigImporter}
import org.silkframework.workspace.{Project, ProjectMarshallingTrait, Task, User}
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsArray
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global

object WorkspaceApi extends Controller {

  def reload = Action {
    User().workspace.reload()
    Ok
  }

  def projects = Action {
    Ok(JsonSerializer.projectsJson)
  }

  def getProject(projectName: String) = Action {
    val project = User().workspace.project(projectName)
    Ok(JsonSerializer.projectJson(project))
  }

  def newProject(project: String) = Action {
    if (User().workspace.projects.exists(_.name == project)) {
      Conflict(JsonError(s"Project with name '$project' already exists. Creation failed."))
    } else {
      val newProject = User().workspace.createProject(project)
      Created(JsonSerializer.projectJson(newProject))
    }
  }

  def deleteProject(project: String) = Action {
    User().workspace.removeProject(project)
    Ok
  }

  def importProject(project: String) = Action { implicit request =>
    for (data <- request.body.asMultipartFormData;
         file <- data.files) {
      // Read the project from the received file
      val inputStream = new FileInputStream(file.ref.file)
      val dotIndex = file.filename.lastIndexOf('.')
      if (dotIndex < 0) {
        throw new IllegalArgumentException("No recognizable file name suffix in uploaded file.")
      }
      val suffix = file.filename.substring(dotIndex + 1)
      val marshallers = marshallingPluginsByFileHandler()
      try {
        marshallers.get(suffix) match {
          case Some(marshaller) =>
            val marshaller = marshallers(suffix)
            val workspace = User().workspace
            workspace.importProject(project, inputStream, marshaller)
          case _ =>
            throw new IllegalArgumentException("No handler found for " + suffix + " files")
        }
      } finally {
        inputStream.close()
      }
    }
    Ok
  }

  /**
    * importProject variant with explicit marshaller parameter
    *
    * @param project
    * @param marshallerId This should be one of the ids returned by the availableProjectMarshallingPlugins method.
    * @return
    */
  def importProjectViaPlugin(project: String, marshallerId: String) = Action { implicit request =>
    val marshallerOpt = marshallingPlugins().filter(_.id == marshallerId).headOption
    marshallerOpt match {
      case Some(marshaller) =>
        for (data <- request.body.asMultipartFormData;
             file <- data.files) {
          // Read the project from the received file
          val inputStream = new FileInputStream(file.ref.file)
          try {
            val workspace = User().workspace
            workspace.importProject(project, inputStream, marshaller)
          } finally {
            inputStream.close()
          }
        }
        Ok
      case _ =>
        BadRequest("No plugin '" + marshallerId + "' found for importing project.")
    }
  }

  def marshallingPluginsByFileHandler(): Map[String, ProjectMarshallingTrait] = {
    marshallingPlugins().map { mp =>
      mp.suffix.map(s => (s, mp))
    }.flatten.toMap
  }

  def marshallingPlugins(): Seq[ProjectMarshallingTrait] = {
    implicit val prefixes = Prefixes.empty
    implicit val resources = EmptyResourceManager
    val pluginConfigs = PluginRegistry.availablePluginsForClass(classOf[ProjectMarshallingTrait])
    pluginConfigs.map(pc =>
      PluginRegistry.create[ProjectMarshallingTrait](pc.id)
    )
  }

  def exportProject(projectName: String) = Action {
    val marshallers = marshallingPlugins()
    val marshaller = marshallers.filter(_.id == "xmlZip").head
    // Export the project into a byte array
    val outputStream = new ByteArrayOutputStream()
    val fileName = User().workspace.exportProject(projectName, outputStream, marshaller)
    val bytes = outputStream.toByteArray
    outputStream.close()

    Ok(bytes).withHeaders("Content-Disposition" -> s"attachment; filename=$fileName")
  }

  def availableProjectMarshallingPlugins(p: String) = Action {
    val marshaller = marshallingPlugins()
    Ok(JsArray(marshaller.map(JsonSerializer.marshaller)))
  }

  def exportProjectViaPlugin(projectName: String, marshallerPluginId: String) = Action {
    val project = User().workspace.project(projectName)
    val marshallerOpt = marshallingPlugins().filter(_.id == marshallerPluginId).headOption
    marshallerOpt match {
      case Some(marshaller) =>
        // Export the project into a byte array
        val outputStream = new ByteArrayOutputStream()
        val fileName = User().workspace.exportProject(projectName, outputStream, marshaller)
        val bytes = outputStream.toByteArray
        outputStream.close()

        Ok(bytes).withHeaders("Content-Disposition" -> s"attachment; filename=$fileName")
      case _ =>
        BadRequest("No plugin '" + marshallerPluginId + "' found for exporting project.")
    }

  }

  def executeProject(projectName: String) = Action {
    val project = User().workspace.project(projectName)
    implicit val prefixes = project.config.prefixes
    implicit val resources = project.resources

    val projectExecutors = PluginRegistry.availablePlugins[ProjectExecutor]
    if (projectExecutors.isEmpty)
      BadRequest("No project executor available")
    else {
      val projectExecutor = projectExecutors.head()
      Activity(projectExecutor.apply(project)).start()
      Ok
    }
  }

  def importLinkSpec(projectName: String) = Action { implicit request => {
    val project = User().workspace.project(projectName)
    implicit val readContext = ReadContext(project.resources)

    request.body match {
      case AnyContentAsMultipartFormData(data) =>
        for (file <- data.files) {
          val config = XmlSerialization.fromXml[LinkingConfig](scala.xml.XML.loadFile(file.ref.file))
          SilkConfigImporter(config, project)
        }
        Ok
      case AnyContentAsXml(xml) =>
        val config = XmlSerialization.fromXml[LinkingConfig](xml.head)
        SilkConfigImporter(config, project)
        Ok
      case _ =>
        UnsupportedMediaType("Link spec must be provided either as Multipart form data or as XML. Please set the Content-Type header accordingly, e.g. to application/xml")
    }
  }
  }

  def exportLinkSpec(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    implicit val prefixes = project.config.prefixes

    val silkConfig = SilkConfigExporter.build(project, task.data)

    Ok(XmlSerialization.toXml(silkConfig))
  }

  def updatePrefixes(project: String) = Action { implicit request => {
    val prefixMap = request.body.asFormUrlEncoded.getOrElse(Map.empty).mapValues(_.mkString)
    val projectObj = User().workspace.project(project)
    projectObj.config = projectObj.config.copy(prefixes = Prefixes(prefixMap))

    Ok
  }
  }

  def getResources(projectName: String) = Action {
    val project = User().workspace.project(projectName)

    Ok(JsonSerializer.projectResources(project))
  }

  def getResource(projectName: String, resourceName: String) = Action {
    val project = User().workspace.project(projectName)
    val resource = project.resources.get(resourceName, mustExist = true)
    val enumerator = Enumerator.fromStream(resource.load)

    Ok.chunked(enumerator).withHeaders("Content-Disposition" -> "attachment")
  }

  def putResource(projectName: String, resourceName: String) = Action { implicit request => {
    val project = User().workspace.project(projectName)
    val resource = project.resources.get(resourceName)

    request.body match {
      case AnyContentAsMultipartFormData(formData) if formData.files.nonEmpty =>
        try {
          val file = formData.files.head.ref.file
          val inputStream = new FileInputStream(file)
          resource.write(inputStream)
          inputStream.close()
          Ok
        } catch {
          case ex: Exception => BadRequest(JsonError(ex))
        }
      case AnyContentAsMultipartFormData(formData) if formData.dataParts.contains("resource-url") =>
        try {
          val dataParts = formData.dataParts("resource-url")
          val url = dataParts.head
          val urlResource = UrlResource(new URL(url))
          val inputStream = urlResource.load
          resource.write(inputStream)
          inputStream.close()
          Ok
        } catch {
          case ex: Exception => BadRequest(JsonError(ex))
        }
      case AnyContentAsRaw(buffer) =>
        val bytes = buffer.asBytes().getOrElse(Array[Byte]())
        resource.write(bytes)
        Ok
      case _ =>
        // Put empty resource
        resource.write(Array[Byte]())
        Ok
    }
  }
  }

  def deleteResource(projectName: String, resourceName: String) = Action {
    val project = User().workspace.project(projectName)
    project.resources.delete(resourceName)

    Ok
  }

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
    val tasks: Seq[Task[_]] = projects.flatMap(_.allTasks)

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

    def taskActivities(task: Task[_]) =
      if (activityName.nonEmpty) task.activity(activityName) :: Nil
      else task.activities

    val projectActivityStreams =
      for (project <- projects; activity <- projectActivities(project)) yield
        Widgets.statusStream(Enumerator(activity.status) andThen Stream.status(activity.control.status), project = project.name, task = "", activity = activity.name)

    val taskActivityStreams =
      for (project <- projects;
           task <- tasks(project);
           activity <- taskActivities(task)) yield
        Widgets.statusStream(Enumerator(activity.status) andThen Stream.status(activity.control.status), project = project.name, task = task.name, activity = activity.name)

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