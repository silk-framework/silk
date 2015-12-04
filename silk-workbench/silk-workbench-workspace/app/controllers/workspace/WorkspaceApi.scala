package controllers.workspace

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, FileInputStream}

import controllers.core.{Stream, Widgets}
import controllers.workspace.Datasets._
import org.silkframework.config._
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.{PluginDescription, PluginRegistry}
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.runtime.serialization.Serialization
import org.silkframework.workspace.Task
import org.silkframework.workspace.io.{SilkConfigExporter, WorkspaceIO, SilkConfigImporter}
import org.silkframework.workspace.activity.ProjectExecutor
import org.silkframework.workspace.xml.XmlWorkspaceProvider
import org.silkframework.workspace.{Project, User}
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global

object WorkspaceApi extends Controller {

  def projects = Action {
    Ok(JsonSerializer.projectsJson)
  }

  def newProject(project: String) = Action {
    User().workspace.createProject(project)
    Ok
  }

  def deleteProject(project: String) = Action {
    User().workspace.removeProject(project)
    Ok
  }

  def importProject(project: String) = Action { implicit request => {
    for(data <- request.body.asMultipartFormData;
        file <- data.files) {
      // Read the project from the received file
      val inputStream = new FileInputStream(file.ref.file)
      if(file.filename.endsWith(".zip")) {
        // We assume that this is a project using the default XML serialization.
        val xmlWorkspace = new XmlWorkspaceProvider(new InMemoryResourceManager())
        xmlWorkspace.importProject(project, inputStream)
        WorkspaceIO.copyProjects(xmlWorkspace, User().workspace.provider)
        User().workspace.reload()
      } else {
        // Try to import the project using the current workspaces import mechanism (if overloaded)
        User().workspace.importProject(project, inputStream)
      }
      inputStream.close()
    }
    Ok
  }}

  def exportProject(project: String) = Action {
    // Export the project into a byte array
    val outputStream = new ByteArrayOutputStream()
    val fileName = User().workspace.exportProject(project, outputStream)
    val bytes = outputStream.toByteArray
    outputStream.close()

    Ok(bytes).withHeaders("Content-Disposition" -> s"attachment; filename=$fileName")
  }

  def executeProject(projectName: String) = Action {
    val project = User().workspace.project(projectName)

    val projectExecutors = PluginRegistry.availablePlugins[ProjectExecutor]
    if(projectExecutors.isEmpty)
      BadRequest("No project executor available")
    else {
      val projectExecutor = projectExecutors.head()
      Activity(projectExecutor.apply(project)).start()
      Ok
    }
  }

  def importLinkSpec(projectName: String) = Action { implicit request => {
    val project = User().workspace.project(projectName)
    implicit val resources = project.resources

    for(data <- request.body.asMultipartFormData;
        file <- data.files) {
      val config = Serialization.fromXml[LinkingConfig](scala.xml.XML.loadFile(file.ref.file))
      SilkConfigImporter(config, project)
    }
    Ok
  }}

  def exportLinkSpec(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)

    val silkConfig = SilkConfigExporter.build(project, task.data)

    Ok(Serialization.toXml(silkConfig))
  }

  def updatePrefixes(project: String) = Action { implicit request => {
    val prefixMap = request.body.asFormUrlEncoded.getOrElse(Map.empty).mapValues(_.mkString)
    val projectObj = User().workspace.project(project)
    projectObj.config = projectObj.config.copy(prefixes = Prefixes(prefixMap))

    Ok
  }}

  def getResources(projectName: String) = Action {
    val project = User().workspace.project(projectName)

    Ok(JsonSerializer.projectResources(project))
  }

  def getResource(projectName: String, resourceName: String) = Action {
    val project = User().workspace.project(projectName)
    val resource = project.resources.get(resourceName)
    val enumerator = Enumerator.fromStream(resource.load)

    Ok.chunked(enumerator).withHeaders("Content-Disposition" -> "attachment")
  }

  def putResource(projectName: String, resourceName: String) = Action { implicit request => {
    val project = User().workspace.project(projectName)

    request.body.asMultipartFormData match {
      case Some(formData) if formData.files.nonEmpty =>
        try {
          val file = formData.files.head.ref.file
          val inputStream = new FileInputStream(file)
          project.resources.get(resourceName).write(inputStream)
          inputStream.close()
          Ok
        } catch {
          case ex: Exception => BadRequest(ex.getMessage)
        }
      case None =>
        // Put empty resource
        project.resources.get(resourceName).write(new ByteArrayInputStream(Array[Byte]()))
        Ok
    }
  }}

  def deleteResource(projectName: String, resourceName: String) = Action {
    val project = User().workspace.project(projectName)
    project.resources.delete(resourceName)

    Ok
  }

  def startActivity(projectName: String, taskName: String, activityName: String) = Action {
    val project = User().workspace.project(projectName)
    val activity =
      if(taskName.nonEmpty) {
        val task = project.anyTask(taskName)
        task.activity(activityName).control
      } else {
        project.activity(activityName).control
      }

    activity.cancel()
    activity.reset()
    activity.start()
    Ok
  }

  def cancelActivity(projectName: String, taskName: String, activityName: String) = Action {
    val project = User().workspace.project(projectName)
    val activity =
      if(taskName.nonEmpty) {
        val task = project.anyTask(taskName)
        task.activity(activityName).control
      } else {
        project.activity(activityName).control
      }

    activity.cancel()
    Ok
  }

  def getActivityConfig(projectName: String, taskName: String, activityName: String) = Action {
    val project = User().workspace.project(projectName)
    val activityConfig =
      if(taskName.nonEmpty) {
        val task = project.anyTask(taskName)
        task.activity(activityName).config
      } else {
        project.activity(activityName).config
      }

    Ok(JsonSerializer.activityConfig(activityConfig))
  }

  def postActivityConfig(projectName: String, taskName: String, activityName: String) = Action { request =>
    request.body match {
      case AnyContentAsFormUrlEncoded(values) =>
        val config = values.mapValues(_.head)
        val project = User().workspace.project(projectName)
        if(taskName.nonEmpty) {
          val task = project.anyTask(taskName)
          task.activity(activityName).update(config)
        } else {
          project.activity(activityName).update(config)
        }
        Ok
      case _ =>
        BadRequest("No config supplied in body.")
    }
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
      else project.activities.map(_.control)

    def taskActivities(task: Task[_]) =
      if (activityName.nonEmpty) task.activity(activityName).control :: Nil
      else task.activities.map(_.control)

    val projectActivityStreams =
      for (project <- projects; activity <- projectActivities(project)) yield
        Widgets.statusStream(Enumerator(activity.status()) andThen Stream.status(activity.status), project = project.name, task = "", activity = activity.name)

    val taskActivityStreams =
      for (project <- projects;
           task <- tasks(project);
           activity <- taskActivities(task)) yield
        Widgets.statusStream(Enumerator(activity.status()) andThen Stream.status(activity.status), project = project.name, task = task.name, activity = activity.name)

    Ok.chunked(Enumerator.interleave(projectActivityStreams ++ taskActivityStreams))
  }

}