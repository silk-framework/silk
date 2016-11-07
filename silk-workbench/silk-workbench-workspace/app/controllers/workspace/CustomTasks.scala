package controllers.workspace

import models.JsonError
import org.silkframework.config.{CustomTask, Task}
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.workspace.User
import play.api.mvc.{Action, Controller}

class CustomTasks extends Controller {

  def getTask(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[CustomTask](taskName)
    val xml = XmlSerialization.toXml(task.data)

    Ok(xml)
  }

  def putTask(projectName: String, taskName: String) = Action { implicit request => {
    val project = User().workspace.project(projectName)
    implicit val readContext = ReadContext(project.resources)
    request.body.asXml match {
      case Some(xml) =>
        try {
          val task = XmlSerialization.fromXml[Task[CustomTask]](xml.head)
          project.updateTask(task.id, task.data)
          Ok
        } catch {
          case ex: Exception =>
            BadRequest(JsonError(ex))
        }
      case None =>
        BadRequest(JsonError("Expecting custom task specification in request body as text/xml."))
    }
  }}

  def deleteTask(project: String, source: String) = Action {
    User().workspace.project(project).removeTask[CustomTask](source)
    Ok
  }

  def taskDialog(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val customTask = if(taskName.isEmpty) None else project.taskOption[CustomTask](taskName).map(p => p.data)
    Ok(views.html.workspace.customTask.customTaskDialog(project, taskName, customTask))
  }
}