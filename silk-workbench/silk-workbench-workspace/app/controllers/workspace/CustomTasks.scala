package controllers.workspace

import org.silkframework.config.{CustomTask, Task}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.runtime.users.WebUserManager
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workbench.utils.ErrorResult
import org.silkframework.workspace.User
import play.api.mvc.{Action, AnyContent, Controller}

class CustomTasks extends Controller {

  def getTask(projectName: String, taskName: String): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    val project = User().workspace.project(projectName)
    val task = project.task[CustomTask](taskName)
    val xml = XmlSerialization.toXml(task.data)

    Ok(xml)
  }

  def pushTask(projectName: String, taskName: String, createOnly: Boolean): Action[AnyContent] = Action { implicit request => {
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    val project = User().workspace.project(projectName)
    implicit val readContext = ReadContext(project.resources)
    request.body.asXml match {
      case Some(xml) =>
        try {
          val task = XmlSerialization.fromXml[Task[CustomTask]](xml.head)
          if(createOnly) {
            project.addTask(task.id, task.data)
          } else {
            project.updateTask(task.id, task.data)
          }
          Ok
        } catch {
          case ex: Exception =>
            ErrorResult(BadUserInputException("Could not parse supplied XML", Some(ex)))
        }
      case None =>
        ErrorResult(BadUserInputException("Expecting custom task specification in request body as text/xml."))
    }
  }}

  def deleteTask(project: String, source: String): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    User().workspace.project(project).removeTask[CustomTask](source)
    Ok
  }

  def taskDialog(projectName: String, taskName: String, createDialog: Boolean): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    val project = User().workspace.project(projectName)
    val customTask = if(taskName.isEmpty) None else project.taskOption[CustomTask](taskName).map(p => p.data)
    Ok(views.html.workspace.customTask.customTaskDialog(project, taskName, customTask, createDialog))
  }
}